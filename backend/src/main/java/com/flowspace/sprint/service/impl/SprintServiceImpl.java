package com.flowspace.sprint.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

import com.flowspace.sprint.dto.request.SprintCreateRequest;
import com.flowspace.sprint.dto.response.SprintResponse;
import com.flowspace.sprint.dto.request.SprintStatusUpdateRequest;
import com.flowspace.sprint.dto.request.SprintUpdateRequest;
import com.flowspace.entity.Block;
import com.flowspace.entity.BlockDatabase;
import com.flowspace.entity.BlockDatabaseColumn;
import com.flowspace.entity.Page;
import com.flowspace.entity.Retrospective;
import com.flowspace.entity.RetrospectiveStatusSnapshot;
import com.flowspace.entity.Sprint;
import com.flowspace.entity.SubTask;
import com.flowspace.entity.SubTaskSnapshot;
import com.flowspace.entity.Task;
import com.flowspace.entity.TaskAssignee;
import com.flowspace.entity.TaskSnapshot;
import com.flowspace.entity.TaskSnapshotAssignee;
import com.flowspace.entity.User;
import com.flowspace.entity.Workspace;
import com.flowspace.entity.WorkspaceMember;
import com.flowspace.entity.WorkspaceTaskStatus;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.entity.enums.BlockType;
import com.flowspace.entity.enums.DatabaseColumnType;
import com.flowspace.entity.enums.DatabaseViewType;
import com.flowspace.entity.enums.NotificationType;
import com.flowspace.entity.enums.SprintStatus;
import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.entity.BlockDatabaseRow;
import com.flowspace.entity.BlockDatabaseCell;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.repository.BlockDatabaseCellRepository;
import com.flowspace.repository.BlockDatabaseColumnRepository;
import com.flowspace.repository.BlockDatabaseRepository;
import com.flowspace.repository.BlockDatabaseRowRepository;
import com.flowspace.repository.BlockRepository;
import com.flowspace.repository.PageRepository;
import com.flowspace.repository.RetrospectiveRepository;
import com.flowspace.repository.RetrospectiveStatusSnapshotRepository;
import com.flowspace.repository.SprintRepository;
import com.flowspace.repository.SubTaskRepository;
import com.flowspace.repository.SubTaskSnapshotRepository;
import com.flowspace.repository.TaskAssigneeRepository;
import com.flowspace.repository.TaskRepository;
import com.flowspace.repository.TaskSnapshotAssigneeRepository;
import com.flowspace.repository.TaskSnapshotRepository;
import com.flowspace.repository.UserRepository;
import com.flowspace.repository.WorkspaceMemberRepository;
import com.flowspace.repository.WorkspaceRepository;
import com.flowspace.repository.WorkspaceTaskStatusRepository;
import com.flowspace.service.ActivityService;
import com.flowspace.service.NotificationService;
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

    private final SprintRepository sprintRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TaskRepository taskRepository;
    private final PageRepository pageRepository;
    private final BlockRepository blockRepository;
    private final BlockDatabaseRepository blockDatabaseRepository;
    private final BlockDatabaseRowRepository blockDatabaseRowRepository;
    private final BlockDatabaseCellRepository blockDatabaseCellRepository;
    private final BlockDatabaseColumnRepository blockDatabaseColumnRepository;
    private final WorkspaceTaskStatusRepository workspaceTaskStatusRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final RetrospectiveStatusSnapshotRepository statusSnapshotRepository;
    private final TaskSnapshotRepository taskSnapshotRepository;
    private final TaskSnapshotAssigneeRepository taskSnapshotAssigneeRepository;
    private final SubTaskSnapshotRepository subTaskSnapshotRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final SubTaskRepository subTaskRepository;
    private final UserRepository userRepository;

    private final ActivityService activityService;
    private final NotificationService notificationService;

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
        activityService.log(workspace, user, ActivityType.SPRINT_CREATED, ActivityTargetType.SPRINT,
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
            completeSprint(sprintId, workspace, user);
        }

        notifySprintStatus(sprint, workspace, after, user);

        return toSprintResponse(sprintMapper.selectSprintProgressById(sprintId));
    }

    // 스프린트 완료: 회고를 만든 뒤, 끝나지 않은 작업 이동과 완료 처리를 프로시저로 해요
    private void completeSprint(Long sprintId, Workspace workspace, User user) {

        // 회고 생성은 아직 JPA 엔티티로 해요(회고 코드를 바꿀 때 함께 정리해요)
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND));

        if (retrospectiveRepository.existsBySprint(sprint)) {
            throw new FlowSpaceException(ErrorCode.RETROSPECTIVE_ALREADY_EXISTS);
        }

        // 회고에는 스프린트의 모든 작업이 기록돼야 해서, 작업을 옮기기 전에 먼저 만들어요
        createRetrospective(sprint, user);

        sprintMapper.updateSprintComplete(sprintId);

        activityService.log(workspace, user, ActivityType.SPRINT_COMPLETED, ActivityTargetType.SPRINT, sprintId);
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

    // 회고 생성
    private void createRetrospective(Sprint sprint, User user) {

        Page page = createRetrospectivePage(sprint, user);

        Retrospective retrospective = retrospectiveRepository
            .save(Retrospective.builder().sprint(sprint).page(page).build());

        createRetrospectiveTable(page, user);

        List<RetrospectiveStatusSnapshot> snapshots = createStatusSnapshots(retrospective, sprint);

        createTaskSnapshots(retrospective, sprint, snapshots);
    }

    // 회고 페이지 생성
    private Page createRetrospectivePage(Sprint sprint, User user) {

        Page page = Page.builder().workspace(sprint.getWorkspace()).parentPage(null).title(sprint.getName() + " 회고")
            .icon("🚀").createdBy(user).build();

        page = pageRepository.save(page);

        activityService.log(sprint.getWorkspace(), user, ActivityType.PAGE_CREATED, ActivityTargetType.PAGE,
            page.getPageId());

        return page;
    }

    // 회고 기본 테이블 생성
    private void createRetrospectiveTable(Page page, User user) {

        BigDecimal position = BigDecimal.valueOf(blockRepository.findByPageOrderByPositionAsc(page).size());

        Block block = Block.builder().page(page).type(BlockType.DATABASE).position(position).createdBy(user).build();

        blockRepository.save(block);

        BlockDatabase database = BlockDatabase.builder().block(block).title("회고").viewType(DatabaseViewType.TABLE)
            .build();

        blockDatabaseRepository.save(database);

        List<BlockDatabaseColumn> columns = new ArrayList<>();

        columns.add(blockDatabaseColumnRepository.save(BlockDatabaseColumn.builder().database(database).name("Keep")
            .type(DatabaseColumnType.TEXT).position(0).build()));

        columns.add(blockDatabaseColumnRepository.save(BlockDatabaseColumn.builder().database(database).name("Problem")
            .type(DatabaseColumnType.TEXT).position(1).build()));

        columns.add(blockDatabaseColumnRepository.save(BlockDatabaseColumn.builder().database(database).name("Try")
            .type(DatabaseColumnType.TEXT).position(2).build()));

        for (int i = 0; i < 4; i++) {

            BlockDatabaseRow row = BlockDatabaseRow.builder().database(database).position(i).build();

            blockDatabaseRowRepository.save(row);

            for (BlockDatabaseColumn column : columns) {

                blockDatabaseCellRepository
                    .save(BlockDatabaseCell.builder().row(row).column(column).value(null).build());
            }
        }
    }

    // 상태 스냅샷 생성
    private List<RetrospectiveStatusSnapshot> createStatusSnapshots(Retrospective retrospective, Sprint sprint) {

        List<WorkspaceTaskStatus> statuses = workspaceTaskStatusRepository
            .findByWorkspaceOrderByPositionAsc(sprint.getWorkspace());

        List<RetrospectiveStatusSnapshot> result = new ArrayList<>();

        for (WorkspaceTaskStatus status : statuses) {

            RetrospectiveStatusSnapshot snapshot = statusSnapshotRepository
                .save(RetrospectiveStatusSnapshot.builder().retrospective(retrospective)
                    .originalStatusId(status.getTaskStatus().getStatusId()).name(status.getTaskStatus().getName())
                    .color(status.getTaskStatus().getColor().name()).position(status.getPosition()).build());

            result.add(snapshot);
        }

        return result;
    }

    // Task 스냅샷 생성
    private void createTaskSnapshots(Retrospective retrospective, Sprint sprint,
        List<RetrospectiveStatusSnapshot> snapshots) {

        Map<Long, RetrospectiveStatusSnapshot> statusMap = snapshots.stream()
            .collect(Collectors.toMap(s -> s.getOriginalStatusId(), s -> s));

        List<Task> tasks = taskRepository.findBySprintOrderByPositionAsc(sprint);

        for (Task task : tasks) {

            TaskSnapshot snapshot = taskSnapshotRepository.save(TaskSnapshot.builder().retrospective(retrospective)
                .snapshotStatus(statusMap.get(task.getStatus().getStatusId())).originalTaskId(task.getTaskId())
                .title(task.getTitle()).priority(task.getPriority().name()).position(task.getPosition()).build());

            createTaskSnapshotAssignees(snapshot, task);
            createSubTaskSnapshots(snapshot, task);
        }
    }

    // Task 담당자 스냅샷 생성
    private void createTaskSnapshotAssignees(TaskSnapshot snapshot, Task task) {

        List<TaskAssignee> assignees = taskAssigneeRepository.findByTaskOrderByTaskAssigneeIdAsc(task);

        for (TaskAssignee assignee : assignees) {

            taskSnapshotAssigneeRepository.save(TaskSnapshotAssignee.builder().snapshot(snapshot)
                .originalUserId(assignee.getUser().getUserId()).nickname(assignee.getUser().getNickname())
                .profileFile(assignee.getUser().getProfileFile()).build());
        }
    }

    // SubTask 스냅샷 생성
    private void createSubTaskSnapshots(TaskSnapshot snapshot, Task task) {

        List<SubTask> subtasks = subTaskRepository.findByTaskOrderByPositionAsc(task);

        for (SubTask subtask : subtasks) {

            User assignee = subtask.getAssignee();

            subTaskSnapshotRepository.save(SubTaskSnapshot.builder().snapshot(snapshot)
                .originalSubtaskId(subtask.getSubtaskId()).content(subtask.getContent())
                .isCompleted(subtask.getIsCompleted()).assigneeName(assignee == null ? null : assignee.getNickname())
                .assigneeProfileFile(assignee == null ? null : assignee.getProfileFile())
                .position(subtask.getPosition()).build());
        }
    }
}