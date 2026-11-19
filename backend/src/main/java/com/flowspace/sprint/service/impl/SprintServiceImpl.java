package com.flowspace.sprint.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.activity.service.ActivityService;
import com.flowspace.common.enums.ActivityTargetType;
import com.flowspace.common.enums.ActivityType;
import com.flowspace.common.enums.NotificationType;
import com.flowspace.common.enums.SprintStatus;
import com.flowspace.common.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.notification.service.NotificationService;
import com.flowspace.retrospective.service.RetrospectiveService;
import com.flowspace.sprint.dto.request.SprintCreateRequest;
import com.flowspace.sprint.dto.request.SprintStatusUpdateRequest;
import com.flowspace.sprint.dto.request.SprintUpdateRequest;
import com.flowspace.sprint.dto.response.SprintResponse;
import com.flowspace.sprint.service.SprintProgressVO;
import com.flowspace.sprint.service.SprintService;
import com.flowspace.sprint.service.SprintVO;
import com.flowspace.user.service.UserVO;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SprintServiceImpl implements SprintService {

    private final WorkspaceService workspaceService;
    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final RetrospectiveService retrospectiveService;

    // 스프린트 SQL
    private final SprintMapper sprintMapper;

    // 스프린트 생성
    @Override
    public SprintResponse createSprint(Long workspaceId, SprintCreateRequest request, String email) {

        UserVO user = findUser(email);

        workspaceService.getWorkspace(workspaceId, email);

        validateSprintDate(request.startDate(), request.endDate());

        if (request.status() == SprintStatus.COMPLETED) {
            throw new FlowSpaceException(ErrorCode.INVALID_SPRINT_STATUS_CHANGE);
        }

        if (request.status() == SprintStatus.ACTIVE) {
            validateNoActiveSprint(workspaceId);
        }

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

        activityService.log(workspaceId, user.getUserId(), ActivityType.SPRINT_CREATED, ActivityTargetType.SPRINT,
            sprint.getSprintId());

        return toSprintResponse(sprintMapper.selectSprintProgressById(sprint.getSprintId()));
    }

    // 워크스페이스 스프린트 목록 조회 (맨 앞은 백로그)
    @Transactional(readOnly = true)
    @Override
    public List<SprintResponse> getSprints(Long workspaceId, String email) {

        workspaceService.getWorkspace(workspaceId, email);

        List<SprintResponse> result = new ArrayList<>();

        result.add(SprintResponse.backlog(workspaceId, sprintMapper.selectBacklogTaskCount(workspaceId)));

        for (SprintProgressVO sprint : sprintMapper.selectSprintProgressList(workspaceId)) {
            result.add(toSprintResponse(sprint));
        }

        return result;
    }

    // 스프린트 단건 조회
    @Transactional(readOnly = true)
    @Override
    public SprintResponse getSprint(Long sprintId, String email) {

        SprintProgressVO sprint = sprintMapper.selectSprintProgressById(sprintId);

        if (sprint == null) {
            throw new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND);
        }

        workspaceService.getWorkspace(sprint.getWorkspaceId(), email);

        return toSprintResponse(sprint);
    }

    // 스프린트 수정
    @Override
    public SprintResponse updateSprint(Long sprintId, SprintUpdateRequest request, String email) {

        SprintVO sprint = findSprint(sprintId);

        workspaceService.getWorkspace(sprint.getWorkspaceId(), email);

        validateSprintDate(request.startDate(), request.endDate());

        sprint.setName(request.name());
        sprint.setGoal(request.goal());
        sprint.setDescription(request.description());
        sprint.setColor(request.color());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());

        sprintMapper.updateSprint(sprint);

        return toSprintResponse(sprintMapper.selectSprintProgressById(sprintId));
    }

    // 스프린트 상태 변경 (계획 → 진행 중 → 완료)
    @Override
    public SprintResponse updateStatus(Long sprintId, SprintStatusUpdateRequest request, String email) {

        UserVO user = findUser(email);
        SprintVO sprint = findSprint(sprintId);

        workspaceService.getWorkspace(sprint.getWorkspaceId(), email);

        SprintStatus before = sprint.getStatus();
        SprintStatus after = request.status();

        if (before == after) {
            return toSprintResponse(sprintMapper.selectSprintProgressById(sprintId));
        }

        if (!isAllowedStatusChange(before, after)) {
            throw new FlowSpaceException(ErrorCode.INVALID_SPRINT_STATUS_CHANGE);
        }

        if (after == SprintStatus.ACTIVE) {
            sprintMapper.updateSprintStart(sprintId);
        } else {
            completeSprint(sprint, user);
        }

        notifySprintStatus(sprint, after, user);

        return toSprintResponse(sprintMapper.selectSprintProgressById(sprintId));
    }

    // 스프린트 완료: 회고 생성 후 미완료 작업 이동·완료 처리 (프로시저)
    private void completeSprint(SprintVO sprint, UserVO user) {

        Long sprintId = sprint.getSprintId();

        retrospectiveService.createRetrospective(sprintId, sprint.getWorkspaceId(), sprint.getName(),
            user.getUserId());

        sprintMapper.updateSprintComplete(sprintId);

        activityService.log(sprint.getWorkspaceId(), user.getUserId(), ActivityType.SPRINT_COMPLETED,
            ActivityTargetType.SPRINT, sprintId);
    }

    // 허용되는 상태 변경 (계획 → 진행 중, 진행 중 → 완료)
    private boolean isAllowedStatusChange(SprintStatus from, SprintStatus to) {

        return (from == SprintStatus.PLANNING && to == SprintStatus.ACTIVE)
            || (from == SprintStatus.ACTIVE && to == SprintStatus.COMPLETED);
    }

    // 스프린트 시작·완료 알림 (워크스페이스 멤버 전체)
    private void notifySprintStatus(SprintVO sprint, SprintStatus after, UserVO actor) {

        List<Long> memberIds = sprintMapper.selectWorkspaceUserIds(sprint.getWorkspaceId());

        if (after == SprintStatus.ACTIVE) {
            notificationService.sendAll(memberIds, actor.getUserId(), sprint.getWorkspaceId(),
                NotificationType.SPRINT_STARTED,
                actor.getNickname() + "님이 '" + sprint.getName() + "' 스프린트를 시작했어요.", sprint.getSprintId(),
                "/sprints/" + sprint.getSprintId() + "/tasks");
        } else if (after == SprintStatus.COMPLETED) {
            notificationService.sendAll(memberIds, actor.getUserId(), sprint.getWorkspaceId(),
                NotificationType.SPRINT_COMPLETED,
                "'" + sprint.getName() + "' 스프린트가 완료되어 회고가 만들어졌어요.", sprint.getSprintId(),
                "/retrospectives/" + sprint.getSprintId());
        }
    }

    // 스프린트 삭제 (관리자 이상)
    @Override
    public void deleteSprint(Long sprintId, String email) {

        SprintVO sprint = findSprint(sprintId);

        WorkspaceRole role = workspaceService.getWorkspace(sprint.getWorkspaceId(), email).role();

        if (!role.isAtLeast(WorkspaceRole.ADMIN)) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }

        sprintMapper.deleteSprint(sprintId);
    }

    // 스프린트 날짜 검증
    private void validateSprintDate(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new FlowSpaceException(ErrorCode.INVALID_SPRINT_DATE);
        }
    }

    // 스프린트 진행률 계산 (작업 수·완료 수는 프로시저 집계)
    private SprintResponse toSprintResponse(SprintProgressVO sprint) {

        int taskCount = sprint.getTaskCount();

        int completedTaskCount = sprint.getCompletedTaskCount();

        int progress = taskCount == 0 ? 0 : (completedTaskCount * 100) / taskCount;

        return SprintResponse.from(sprint, progress, taskCount, completedTaskCount);
    }

    // 진행 중인 스프린트 중복 방지 (스프린트 생성 시)
    private void validateNoActiveSprint(Long workspaceId) {

        if (sprintMapper.selectSprintCountByStatus(workspaceId, SprintStatus.ACTIVE) > 0) {
            throw new FlowSpaceException(ErrorCode.SPRINT_ALREADY_ACTIVE);
        }
    }

    // ---------- 조회 + 없으면 예외 ----------

    private UserVO findUser(String email) {

        UserVO user = sprintMapper.selectUserByEmail(email);

        if (user == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }

    private SprintVO findSprint(Long sprintId) {

        SprintVO sprint = sprintMapper.selectSprintById(sprintId);

        if (sprint == null) {
            throw new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND);
        }

        return sprint;
    }
}
