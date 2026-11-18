package com.flowspace.sprint.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import com.flowspace.sprint.dto.request.SprintCreateRequest;
import com.flowspace.sprint.dto.response.SprintResponse;
import com.flowspace.sprint.dto.request.SprintStatusUpdateRequest;
import com.flowspace.sprint.dto.request.SprintUpdateRequest;
import com.flowspace.entity.User;
import com.flowspace.entity.Workspace;
import com.flowspace.entity.WorkspaceMember;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.entity.enums.NotificationType;
import com.flowspace.entity.enums.SprintStatus;
import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.repository.TaskRepository;
import com.flowspace.repository.UserRepository;
import com.flowspace.repository.WorkspaceMemberRepository;
import com.flowspace.repository.WorkspaceRepository;
import com.flowspace.activity.service.ActivityService;
import com.flowspace.notification.service.NotificationService;
import com.flowspace.retrospective.service.RetrospectiveService;
import com.flowspace.sprint.service.SprintService;
import com.flowspace.sprint.service.SprintProgressVO;
import com.flowspace.sprint.service.SprintVO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SprintServiceImpl implements SprintService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final RetrospectiveService retrospectiveService;

    // 스프린트 조회용 MyBatis Mapper (같은 패키지라 import가 없어요)
    private final SprintMapper sprintMapper;

    // 스프린트 생성
    @Override
    public SprintResponse createSprint(Long workspaceId, SprintCreateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        validateMember(workspace, user);
        validateSprintDate(request.startDate(), request.endDate());

        // 완료된 스프린트를 처음부터 만들 수는 없어요(완료는 진행 중인 스프린트를 끝낼 때만 — 회고가 만들어져요).
        if (request.status() == SprintStatus.COMPLETED) {
            throw new FlowSpaceException(ErrorCode.INVALID_SPRINT_STATUS_CHANGE);
        }

        // 진행 중인 스프린트는 워크스페이스에서 하나만 둬요(칸반도 그 스프린트 하나를 보여줘요).
        if (request.status() == SprintStatus.ACTIVE) {
            validateNoActiveSprint(workspaceId);
        }

        // 등록할 값을 VO에 담아요. 워크스페이스와 작성자는 객체가 아니라 번호로 넣어요.
        SprintVO sprint = new SprintVO();
        sprint.setWorkspaceId(workspaceId);
        sprint.setCreatedBy(user.getUserId());
        sprint.setName(request.name());
        sprint.setGoal(request.goal());
        sprint.setDescription(request.description());
        sprint.setColor(request.color());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        sprint.setStatus(request.status());

        sprintMapper.insertSprint(sprint);

        // INSERT가 끝나면 DB가 만든 번호가 sprint.getSprintId()에 들어 있어요
        activityService.log(workspaceId, user.getUserId(), ActivityType.SPRINT_CREATED, ActivityTargetType.SPRINT,
            sprint.getSprintId());

        // 만든 날짜는 DB가 채웠기 때문에 다시 조회해서 돌려줘요
        return toSprintResponse(sprintMapper.selectSprintProgressById(sprint.getSprintId()));
    }

    // 워크스페이스 스프린트 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<SprintResponse> getSprints(Long workspaceId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        validateMember(workspace, user);

        List<SprintResponse> result = new ArrayList<>();

        long backlogCount = taskRepository.countByWorkspaceAndSprintIsNull(workspace);

        result.add(SprintResponse.backlog(workspace.getWorkspaceId(), (int) backlogCount));

        // 스프린트 목록과 작업 수·완료 수를 프로시저 한 번으로 가져와요
        for (SprintProgressVO sprint : sprintMapper.selectSprintProgressList(workspaceId)) {
            result.add(toSprintResponse(sprint));
        }

        return result;
    }

    // 스프린트 단건 조회
    @Transactional(readOnly = true)
    @Override
    public SprintResponse getSprint(Long sprintId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        // MyBatis는 결과가 없으면 null을 돌려줘서 직접 확인해요
        SprintProgressVO sprint = sprintMapper.selectSprintProgressById(sprintId);

        if (sprint == null) {
            throw new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND);
        }

        // VO에는 워크스페이스 번호만 있어서, 멤버 확인에 쓸 워크스페이스를 따로 조회해요
        Workspace workspace = workspaceRepository.findById(sprint.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        validateMember(workspace, user);

        return toSprintResponse(sprint);
    }

    // 스프린트 수정
    @Override
    public SprintResponse updateSprint(Long sprintId, SprintUpdateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        SprintVO sprint = sprintMapper.selectSprintById(sprintId);

        if (sprint == null) {
            throw new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND);
        }

        Workspace workspace = workspaceRepository.findById(sprint.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        validateMember(workspace, user);
        validateSprintDate(request.startDate(), request.endDate());

        sprint.setName(request.name());
        sprint.setGoal(request.goal());
        sprint.setDescription(request.description());
        sprint.setColor(request.color());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());

        // JPA는 값만 바꾸면 끝날 때 알아서 UPDATE 했지만, MyBatis는 직접 호출해야 DB에 반영돼요
        sprintMapper.updateSprint(sprint);

        return toSprintResponse(sprintMapper.selectSprintProgressById(sprintId));
    }

    // 스프린트 상태 변경
    @Override
    public SprintResponse updateStatus(Long sprintId, SprintStatusUpdateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        SprintVO sprint = sprintMapper.selectSprintById(sprintId);

        if (sprint == null) {
            throw new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND);
        }

        Workspace workspace = workspaceRepository.findById(sprint.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        validateMember(workspace, user);

        SprintStatus before = sprint.getStatus();
        SprintStatus after = request.status();

        // 같은 상태로 바꾸는 요청은 아무것도 바꾸지 않아요
        if (before == after) {
            return toSprintResponse(sprintMapper.selectSprintProgressById(sprintId));
        }

        // 계획 → 진행 중 → 완료 순서로만 바꿀 수 있어요(완료된 스프린트를 되살리거나, 시작 없이 완료하면 회고·Task 정리가 어긋나요).
        if (!isAllowedStatusChange(before, after)) {
            throw new FlowSpaceException(ErrorCode.INVALID_SPRINT_STATUS_CHANGE);
        }

        if (after == SprintStatus.ACTIVE) {
            // 진행 중이던 다른 스프린트를 계획됨으로 돌리고 이 스프린트를 시작하는 일을 프로시저 한 번으로 해요
            sprintMapper.updateSprintStart(sprintId);
        } else {
            completeSprint(sprint, workspace, user);
        }

        notifySprintStatus(sprint, workspace, after, user);

        return toSprintResponse(sprintMapper.selectSprintProgressById(sprintId));
    }

    // 스프린트 완료: 회고를 만든 뒤, 끝나지 않은 작업 이동과 완료 처리를 프로시저로 해요
    private void completeSprint(SprintVO sprint, Workspace workspace, User user) {

        Long sprintId = sprint.getSprintId();

        // 회고에는 스프린트의 모든 작업이 기록돼야 해서, 작업을 옮기기 전에 먼저 만들어요(이미 있으면 회고 서비스가 거절해요)
        retrospectiveService.createRetrospective(sprintId, workspace.getWorkspaceId(), sprint.getName(),
            user.getUserId());

        sprintMapper.updateSprintComplete(sprintId);

        activityService.log(workspace.getWorkspaceId(), user.getUserId(), ActivityType.SPRINT_COMPLETED,
            ActivityTargetType.SPRINT, sprintId);
    }

    private boolean isAllowedStatusChange(SprintStatus from, SprintStatus to) {

        return (from == SprintStatus.PLANNING && to == SprintStatus.ACTIVE)
            || (from == SprintStatus.ACTIVE && to == SprintStatus.COMPLETED);
    }

    // 스프린트 시작/완료를 워크스페이스 멤버에게 알림
    private void notifySprintStatus(SprintVO sprint, Workspace workspace, SprintStatus after, User actor) {

        List<User> members = workspaceMemberRepository.findByWorkspace(workspace).stream()
            .map(m -> m.getUser()).toList();

        if (after == SprintStatus.ACTIVE) {
            notificationService.sendAll(members, actor, workspace, NotificationType.SPRINT_STARTED,
                actor.getNickname() + "님이 '" + sprint.getName() + "' 스프린트를 시작했어요.", sprint.getSprintId(),
                "/sprints/" + sprint.getSprintId() + "/tasks");
        } else if (after == SprintStatus.COMPLETED) {
            notificationService.sendAll(members, actor, workspace, NotificationType.SPRINT_COMPLETED,
                "'" + sprint.getName() + "' 스프린트가 완료되어 회고가 만들어졌어요.", sprint.getSprintId(),
                "/retrospectives/" + sprint.getSprintId());
        }
    }

    // 스프린트 삭제
    @Override
    public void deleteSprint(Long sprintId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        SprintVO sprint = sprintMapper.selectSprintById(sprintId);

        if (sprint == null) {
            throw new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND);
        }

        Workspace workspace = workspaceRepository.findById(sprint.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        // 스프린트 삭제는 관리자 이상만 할 수 있어요.
        validateAdmin(workspace, user);

        // 작업을 백로그로 옮기고 스프린트를 지우는 일을 프로시저 한 번으로 해요
        sprintMapper.deleteSprint(sprintId);
    }

    // 워크스페이스 멤버 확인
    private void validateMember(Workspace workspace, User user) {
        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));
    }

    // 워크스페이스 관리자 이상 확인
    private void validateAdmin(Workspace workspace, User user) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        if (!member.getRole().isAtLeast(WorkspaceRole.ADMIN)) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }
    }

    // 스프린트 날짜 검증
    private void validateSprintDate(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new FlowSpaceException(ErrorCode.INVALID_SPRINT_DATE);
        }
    }

    // 스프린트 진행률 계산 (작업 수와 완료 수는 프로시저가 세어 와요)
    private SprintResponse toSprintResponse(SprintProgressVO sprint) {

        int taskCount = sprint.getTaskCount();

        int completedTaskCount = sprint.getCompletedTaskCount();

        int progress = taskCount == 0 ? 0 : (completedTaskCount * 100) / taskCount;

        return SprintResponse.from(sprint, progress, taskCount, completedTaskCount);
    }

    // 진행 중인 스프린트가 이미 있으면 막아요(스프린트를 처음 만들 때만 써요)
    private void validateNoActiveSprint(Long workspaceId) {

        if (sprintMapper.selectSprintCountByStatus(workspaceId, SprintStatus.ACTIVE) > 0) {
            throw new FlowSpaceException(ErrorCode.SPRINT_ALREADY_ACTIVE);
        }
    }
}
