package com.flowspace.task.service.impl;

import com.flowspace.activity.service.ActivityService;
import com.flowspace.file.service.FileService;
import com.flowspace.notification.service.NotificationService;
import com.flowspace.task.service.SubTaskVO;
import com.flowspace.task.service.TaskAssigneeVO;
import com.flowspace.task.service.TaskService;
import com.flowspace.task.service.TaskStatusVO;
import com.flowspace.task.service.TaskVO;
import com.flowspace.task.dto.response.AssigneeItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.flowspace.task.dto.request.TaskCreateRequest;
import com.flowspace.task.dto.request.TaskReorderRequest;
import com.flowspace.task.dto.response.TaskResponse;
import com.flowspace.task.dto.response.TaskSearchResponse;
import com.flowspace.task.dto.request.TaskSprintUpdateRequest;
import com.flowspace.task.dto.request.TaskStatusCreateRequest;
import com.flowspace.task.dto.request.TaskStatusDeleteRequest;
import com.flowspace.task.dto.request.TaskStatusEditRequest;
import com.flowspace.task.dto.request.TaskStatusReorderRequest;
import com.flowspace.task.dto.response.TaskStatusResponse;
import com.flowspace.task.dto.request.TaskStatusUpdateRequest;
import com.flowspace.task.dto.request.TaskUpdateRequest;
import com.flowspace.comment.dto.response.CommentResponse;
import com.flowspace.comment.service.CommentService;
import com.flowspace.task.dto.request.SubTaskCreateRequest;
import com.flowspace.task.dto.request.SubTaskReorderRequest;
import com.flowspace.task.dto.response.SubTaskResponse;
import com.flowspace.task.dto.request.SubTaskUpdateRequest;
import com.flowspace.entity.Sprint;
import com.flowspace.entity.User;
import com.flowspace.entity.Workspace;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.entity.enums.NotificationType;
import com.flowspace.entity.enums.TaskStatusCategory;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.repository.SprintRepository;
import com.flowspace.repository.UserRepository;
import com.flowspace.repository.WorkspaceMemberRepository;
import com.flowspace.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final SprintRepository sprintRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final FileService fileService;
    private final UserRepository userRepository;

    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final CommentService commentService;

    // 작업 SQL
    private final TaskMapper taskMapper;

    // 작업 삭제 시 업로드 파일 번호 조회
    private final TaskAttachmentMapper taskAttachmentMapper;

    // Task 상태 생성
    @Override
    public TaskStatusResponse createStatus(Long workspaceId, TaskStatusCreateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        TaskStatusVO status = new TaskStatusVO();
        status.setWorkspaceId(workspaceId);
        status.setName(request.name());
        status.setCategory(request.category());
        status.setColor(request.color());
        status.setWipLimit(request.wipLimit());
        status.setPosition(taskMapper.selectTaskStatusCount(workspaceId));

        taskMapper.insertTaskStatus(status);
        taskMapper.insertWorkspaceTaskStatus(status);

        return TaskStatusResponse.from(findTaskStatus(workspaceId, status.getStatusId()));
    }

    // Task 상태 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<TaskStatusResponse> getStatuses(Long workspaceId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        return taskMapper.selectTaskStatusList(workspaceId).stream().map(TaskStatusResponse::from).toList();
    }

    // Task 상태 수정
    @Override
    public TaskStatusResponse updateStatusInfo(Long statusId, TaskStatusEditRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Long workspaceId = request.workspaceId();

        TaskStatusVO status = findTaskStatus(workspaceId, statusId);

        validateWorkspaceMember(workspaceId, user);

        if (status.getIsDefault()) {

            TaskStatusVO newStatus = new TaskStatusVO();
            newStatus.setName(request.name());
            newStatus.setCategory(request.category());
            newStatus.setColor(request.color());

            taskMapper.insertTaskStatus(newStatus);

            taskMapper.updateDefaultTaskStatus(workspaceId, statusId, newStatus.getStatusId(), request.wipLimit());

            return TaskStatusResponse.from(findTaskStatus(workspaceId, newStatus.getStatusId()));
        }

        status.setName(request.name());
        status.setCategory(request.category());
        status.setColor(request.color());
        status.setWipLimit(request.wipLimit());

        taskMapper.updateTaskStatusInfo(status);
        taskMapper.updateWorkspaceTaskStatusWipLimit(status);

        return TaskStatusResponse.from(status);
    }

    // Task 상태 순서 변경
    @Override
    public void reorderStatuses(TaskStatusReorderRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Long workspaceId = request.workspaceId();

        validateWorkspaceMember(workspaceId, user);

        for (TaskStatusReorderRequest.Item item : request.statuses()) {

            int updated = taskMapper.updateTaskStatusPosition(workspaceId, item.statusId(), item.position());

            if (updated == 0) {
                throw new FlowSpaceException(ErrorCode.TASK_STATUS_NOT_FOUND);
            }
        }
    }

    // Task 상태 삭제
    @Override
    public void deleteStatus(Long statusId, TaskStatusDeleteRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Long workspaceId = request.workspaceId();

        findTaskStatus(workspaceId, statusId);

        validateWorkspaceMember(workspaceId, user);

        if (taskMapper.selectTaskStatusCount(workspaceId) <= 1) {
            throw new FlowSpaceException(ErrorCode.LAST_TASK_STATUS_CANNOT_DELETE);
        }

        if (statusId.equals(request.targetStatusId())) {
            throw new FlowSpaceException(ErrorCode.INVALID_TASK_STATUS);
        }

        findTaskStatus(workspaceId, request.targetStatusId());

        taskMapper.deleteTaskStatus(workspaceId, statusId, request.targetStatusId());
    }

    // Task 생성
    @Override
    public TaskResponse createTask(Long sprintId, TaskCreateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(sprint.getWorkspace(), user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        return saveNewTask(sprint.getWorkspace(), sprint, request, user);
    }

    // 백로그 작업 생성 (스프린트 없음)
    @Override
    public TaskResponse createBacklogTask(Long workspaceId, TaskCreateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        return saveNewTask(workspace, null, request, user);
    }

    // Task 저장 공통 처리 (sprint가 null이면 Backlog Task)
    private TaskResponse saveNewTask(Workspace workspace, Sprint sprint, TaskCreateRequest request, User user) {

        TaskStatusVO status = validateWorkspaceStatus(workspace, request.statusId());

        BigDecimal position = BigDecimal
            .valueOf(taskMapper.selectTaskCountByStatus(workspace.getWorkspaceId(), status.getStatusId()));

        int taskNumber = taskMapper.selectMaxTaskNumber(workspace.getWorkspaceId()) + 1;

        TaskVO task = new TaskVO();
        task.setWorkspaceId(workspace.getWorkspaceId());
        task.setSprintId(sprint == null ? null : sprint.getSprintId());
        task.setStatusId(status.getStatusId());
        task.setCreatedBy(user.getUserId());
        task.setTaskNumber(taskNumber);
        task.setPosition(position);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStartDate(request.startDate());
        task.setEndDate(request.endDate());
        task.setPriority(request.priority());

        taskMapper.insertTask(task);

        activityService.log(workspace.getWorkspaceId(), user.getUserId(), ActivityType.TASK_CREATED,
            ActivityTargetType.TASK, task.getTaskId());

        List<Long> assigneeIds = request.assigneeIds() == null ? List.of() : request.assigneeIds();

        for (Long assigneeId : assigneeIds) {

            User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

            workspaceMemberRepository.findByWorkspaceAndUser(workspace, assignee)
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

            taskMapper.insertTaskAssignee(task.getTaskId(), assignee.getUserId());

            notifyAssigned(task, workspace, assignee, user);
        }

        return toTaskResponse(findTask(task.getTaskId()));
    }

    // 스프린트 Task 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<TaskResponse> getTasksBySprint(Long sprintId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(sprint.getWorkspace(), user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        return toTaskResponses(sprint.getWorkspace().getWorkspaceId(), sprintId);
    }

    // Task 단건 조회
    @Transactional(readOnly = true)
    @Override
    public TaskResponse getTask(Long taskId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        TaskVO task = findTask(taskId);

        Workspace workspace = workspaceRepository.findById(task.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        return toTaskResponse(task);
    }

    // Task 수정
    @Override
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        TaskVO task = findTask(taskId);

        Workspace workspace = workspaceRepository.findById(task.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        if (request.sprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.sprintId())
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND));

            if (!sprint.getWorkspace().getWorkspaceId().equals(task.getWorkspaceId())) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }
        }

        TaskStatusVO status = validateWorkspaceStatus(workspace, request.statusId());

        boolean sprintChanged = !Objects.equals(task.getSprintId(), request.sprintId());

        boolean statusChanged = !task.getStatusId().equals(request.statusId());

        if (sprintChanged || statusChanged) {
            task.setPosition(BigDecimal
                .valueOf(taskMapper.selectTaskCountByStatus(task.getWorkspaceId(), status.getStatusId())));
        }

        syncCompletedAt(task, status);

        task.setSprintId(request.sprintId());
        task.setStatusId(status.getStatusId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStartDate(request.startDate());
        task.setEndDate(request.endDate());
        task.setPriority(request.priority());

        taskMapper.updateTask(task);

        Set<Long> previousAssigneeIds = Set.copyOf(taskMapper.selectTaskAssigneeUserIds(taskId));

        taskMapper.deleteTaskAssignees(taskId);

        List<Long> assigneeIds = request.assigneeIds() == null ? List.of() : request.assigneeIds();

        List<User> keptAssignees = new ArrayList<>();

        for (Long assigneeId : assigneeIds) {

            User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

            workspaceMemberRepository.findByWorkspaceAndUser(workspace, assignee)
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

            taskMapper.insertTaskAssignee(taskId, assignee.getUserId());

            if (previousAssigneeIds.contains(assignee.getUserId())) {
                keptAssignees.add(assignee);
            } else {
                notifyAssigned(task, workspace, assignee, user);
            }
        }

        taskMapper.updateSubTaskAssigneeClear(taskId, assigneeIds);

        if (statusChanged) {
            notifyStatusChanged(task, workspace, status, user, keptAssignees);
        }

        return toTaskResponse(findTask(taskId));
    }

    // Backlog Task 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<TaskResponse> getBacklogTasks(Long workspaceId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        return toTaskResponses(workspaceId, null);
    }

    // Task 상태 변경
    @Override
    public TaskResponse updateTaskStatus(Long taskId, TaskStatusUpdateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        TaskVO task = findTask(taskId);

        Workspace workspace = workspaceRepository.findById(task.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        TaskStatusVO status = validateWorkspaceStatus(workspace, request.statusId());

        if (task.getStatusId().equals(request.statusId())) {
            return toTaskResponse(task);
        }

        task.setPosition(BigDecimal
            .valueOf(taskMapper.selectTaskCountByStatus(task.getWorkspaceId(), status.getStatusId())));
        task.setStatusId(status.getStatusId());
        syncCompletedAt(task, status);

        taskMapper.updateTask(task);

        if (status.getCategory() == TaskStatusCategory.DONE) {
            activityService.log(workspace.getWorkspaceId(), user.getUserId(), ActivityType.TASK_COMPLETED,
                ActivityTargetType.TASK, taskId);
        }

        notifyStatusChanged(task, workspace, status, user, findAssigneeUsers(taskId));

        return toTaskResponse(findTask(taskId));
    }

    // Task 삭제
    @Override
    public void deleteTask(Long taskId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        TaskVO task = findTask(taskId);

        Workspace workspace = workspaceRepository.findById(task.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        List<Long> attachmentFileIds = taskAttachmentMapper.selectTaskAttachmentFileIds(taskId);

        taskMapper.deleteTask(taskId);

        for (Long fileId : attachmentFileIds) {
            fileService.delete(fileId);
        }
    }

    // SubTask 생성
    @Override
    public SubTaskResponse createSubTask(Long taskId, SubTaskCreateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        TaskVO task = findTask(taskId);

        validateTaskMember(task, user);

        validateSubTaskAssignee(taskId, request.assigneeId());

        SubTaskVO subTask = new SubTaskVO();
        subTask.setTaskId(taskId);
        subTask.setAssigneeId(request.assigneeId());
        subTask.setContent(request.content());
        subTask.setPosition(taskMapper.selectSubTaskCount(taskId));

        taskMapper.insertSubTask(subTask);

        return SubTaskResponse.from(findSubTask(subTask.getSubtaskId()));
    }

    // SubTask 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<SubTaskResponse> getSubTasks(Long taskId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        validateTaskMember(findTask(taskId), user);

        return taskMapper.selectSubTaskList(List.of(taskId)).stream().map(SubTaskResponse::from).toList();
    }

    // SubTask 수정
    @Override
    public SubTaskResponse updateSubTask(Long subTaskId, SubTaskUpdateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        SubTaskVO subTask = findSubTask(subTaskId);

        validateTaskMember(findTask(subTask.getTaskId()), user);

        validateSubTaskAssignee(subTask.getTaskId(), request.assigneeId());

        subTask.setContent(request.content());
        subTask.setAssigneeId(request.assigneeId());
        subTask.setIsCompleted(request.isCompleted());

        taskMapper.updateSubTask(subTask);

        return SubTaskResponse.from(subTask);
    }

    // SubTask 순서 변경
    @Override
    public void reorderSubTasks(SubTaskReorderRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        for (SubTaskReorderRequest.Item item : request.subtasks()) {

            SubTaskVO subTask = findSubTask(item.subtaskId());

            validateTaskMember(findTask(subTask.getTaskId()), user);

            taskMapper.updateSubTaskPosition(item.subtaskId(), item.position());
        }
    }

    // SubTask 삭제
    @Override
    public void deleteSubTask(Long subtaskId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        SubTaskVO subTask = findSubTask(subtaskId);

        validateTaskMember(findTask(subTask.getTaskId()), user);

        taskMapper.deleteSubTask(subtaskId);
    }

    // Task 스프린트 이동
    @Override
    public TaskResponse updateTaskSprint(Long taskId, TaskSprintUpdateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        TaskVO task = findTask(taskId);

        Workspace workspace = workspaceRepository.findById(task.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        if (Objects.equals(task.getSprintId(), request.sprintId())) {
            return toTaskResponse(task);
        }

        if (request.sprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.sprintId())
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND));

            if (!sprint.getWorkspace().getWorkspaceId().equals(task.getWorkspaceId())) {
                throw new FlowSpaceException(ErrorCode.INVALID_SPRINT);
            }
        }

        task.setPosition(BigDecimal
            .valueOf(taskMapper.selectTaskCountByStatus(task.getWorkspaceId(), task.getStatusId())));
        task.setSprintId(request.sprintId());

        taskMapper.updateTask(task);

        return toTaskResponse(findTask(taskId));
    }

    // Task 순서 변경 (칸반에서 끌어다 놓기)
    @Override
    public void reorderTasks(TaskReorderRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        for (TaskReorderRequest.Item item : request.tasks()) {

            TaskVO task = findTask(item.taskId());

            Workspace workspace = workspaceRepository.findById(task.getWorkspaceId())
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

            workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

            TaskStatusVO status = validateWorkspaceStatus(workspace, item.statusId());

            boolean statusChanged = !task.getStatusId().equals(status.getStatusId());

            task.setStatusId(status.getStatusId());
            task.setPosition(item.position());
            syncCompletedAt(task, status);

            taskMapper.updateTask(task);

            if (statusChanged) {
                notifyStatusChanged(task, workspace, status, user, findAssigneeUsers(task.getTaskId()));
            }
        }
    }

    // 담당자로 지정된 사람에게 알림
    private void notifyAssigned(TaskVO task, Workspace workspace, User assignee, User actor) {
        notificationService.send(assignee.getUserId(), actor.getUserId(), workspace.getWorkspaceId(),
            NotificationType.TASK_ASSIGNED,
            actor.getNickname() + "님이 '" + task.getTitle() + "' 작업의 담당자로 지정했어요.", task.getTaskId(),
            NotificationService.taskLink(task.getSprintId(), task.getTaskId()));
    }

    // 작업 상태 변경 알림 (변경한 본인 제외)
    private void notifyStatusChanged(TaskVO task, Workspace workspace, TaskStatusVO status, User actor,
        List<User> targets) {
        notificationService.sendAll(targets.stream().map(User::getUserId).toList(), actor.getUserId(),
            workspace.getWorkspaceId(), NotificationType.TASK_STATUS_CHANGED,
            actor.getNickname() + "님이 '" + task.getTitle() + "' 작업을 '" + status.getName() + "' 상태로 바꿨어요.",
            task.getTaskId(), NotificationService.taskLink(task.getSprintId(), task.getTaskId()));
    }

    // 완료 시각: 완료 상태가 되면 처음 한 번만 기록하고, 완료가 아니면 비워요(엔티티의 syncCompletedAt과 같은 규칙)
    private void syncCompletedAt(TaskVO task, TaskStatusVO status) {

        if (status.getCategory() != TaskStatusCategory.DONE) {
            task.setCompletedAt(null);
        } else if (task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        }
    }

    // 작업 담당자 (알림 받을 사람)
    private List<User> findAssigneeUsers(Long taskId) {
        return userRepository.findAllById(taskMapper.selectTaskAssigneeUserIds(taskId));
    }

    // 작업 상태가 해당 워크스페이스의 상태인지 확인 (아니면 INVALID_TASK_STATUS)
    private TaskStatusVO validateWorkspaceStatus(Workspace workspace, Long statusId) {

        TaskStatusVO status = taskMapper.selectTaskStatusById(workspace.getWorkspaceId(), statusId);

        if (status == null) {
            throw new FlowSpaceException(ErrorCode.INVALID_TASK_STATUS);
        }

        return status;
    }

    // 상태 화면에서 고르는 상태 한 개 (없으면 TASK_STATUS_NOT_FOUND)
    private TaskStatusVO findTaskStatus(Long workspaceId, Long statusId) {

        TaskStatusVO status = taskMapper.selectTaskStatusById(workspaceId, statusId);

        if (status == null) {
            throw new FlowSpaceException(ErrorCode.TASK_STATUS_NOT_FOUND);
        }

        return status;
    }

    // 워크스페이스 멤버 확인
    private void validateWorkspaceMember(Long workspaceId, User user) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));
    }

    // Task 검색
    @Transactional(readOnly = true)
    @Override
    public List<TaskSearchResponse> searchTasks(Long workspaceId, String keyword, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        String search = keyword == null ? "" : keyword;

        String escaped = search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

        List<TaskVO> tasks = taskMapper.selectTaskSearchList(workspaceId, escaped);

        if (tasks.isEmpty()) {
            return List.of();
        }

        List<Long> taskIds = tasks.stream().map(TaskVO::getTaskId).toList();

        Map<Long, List<AssigneeItem>> assigneesByTask =
            groupAssigneeItems(taskMapper.selectTaskAssigneeListByTaskIds(taskIds));

        return tasks.stream()
            .map(task -> TaskSearchResponse.from(task, assigneesByTask.getOrDefault(task.getTaskId(), List.of())))
            .toList();
    }

    // 작업 목록 응답 변환 (sprintId가 null이면 백로그)
    // 작업·담당자는 프로시저, 하위 작업은 XML, 댓글은 댓글 서비스에서 한 번씩 조회
    private List<TaskResponse> toTaskResponses(Long workspaceId, Long sprintId) {

        List<TaskVO> tasks = taskMapper.selectTaskList(workspaceId, sprintId);

        if (tasks.isEmpty()) {
            return List.of();
        }

        List<Long> taskIds = tasks.stream().map(TaskVO::getTaskId).toList();

        Map<Long, List<AssigneeItem>> assigneesByTask =
            groupAssigneeItems(taskMapper.selectTaskAssigneeList(workspaceId, sprintId));

        Map<Long, List<SubTaskResponse>> subtasksByTask = new HashMap<>();

        for (SubTaskVO subTask : taskMapper.selectSubTaskList(taskIds)) {
            subtasksByTask.computeIfAbsent(subTask.getTaskId(), key -> new ArrayList<>())
                .add(SubTaskResponse.from(subTask));
        }

        Map<Long, List<CommentResponse>> commentsByTask = commentService.getTaskCommentMap(taskIds);

        return tasks.stream()
            .map(task -> TaskResponse.from(task, assigneesByTask.getOrDefault(task.getTaskId(), List.of()),
                subtasksByTask.getOrDefault(task.getTaskId(), List.of()),
                commentsByTask.getOrDefault(task.getTaskId(), List.of())))
            .toList();
    }

    // 작업 한 건 (없으면 예외)
    private TaskVO findTask(Long taskId) {

        TaskVO task = taskMapper.selectTaskById(taskId);

        if (task == null) {
            throw new FlowSpaceException(ErrorCode.TASK_NOT_FOUND);
        }

        return task;
    }

    // 하위 작업 한 건 (없으면 예외)
    private SubTaskVO findSubTask(Long subtaskId) {

        SubTaskVO subTask = taskMapper.selectSubTaskById(subtaskId);

        if (subTask == null) {
            throw new FlowSpaceException(ErrorCode.SUBTASK_NOT_FOUND);
        }

        return subTask;
    }

    // 작업이 속한 워크스페이스의 멤버 확인 후 워크스페이스 반환
    private Workspace validateTaskMember(TaskVO task, User user) {

        Workspace workspace = workspaceRepository.findById(task.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        return workspace;
    }

    // 하위 작업 담당자는 작업 담당자 중에서만 지정 (담당자가 없으면 생략)
    private void validateSubTaskAssignee(Long taskId, Long assigneeId) {

        if (assigneeId == null) {
            return;
        }

        userRepository.findById(assigneeId).orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        if (taskMapper.selectTaskAssigneeCount(taskId, assigneeId) == 0) {
            throw new FlowSpaceException(ErrorCode.INVALID_SUBTASK_ASSIGNEE);
        }
    }

    // 작업 한 건 응답 변환 (목록과 같은 방식)
    private TaskResponse toTaskResponse(TaskVO task) {

        Long taskId = task.getTaskId();

        List<AssigneeItem> assignees = taskMapper.selectTaskAssigneeListByTaskId(taskId).stream()
            .map(AssigneeItem::from).toList();

        List<SubTaskResponse> subtasks = taskMapper.selectSubTaskList(List.of(taskId)).stream()
            .map(SubTaskResponse::from).toList();

        List<CommentResponse> comments =
            commentService.getTaskCommentMap(List.of(taskId)).getOrDefault(taskId, List.of());

        return TaskResponse.from(task, assignees, subtasks, comments);
    }

    // 담당자 조회 결과를 작업 번호별로 묶음 (칸반 목록·검색 공용)
    private Map<Long, List<AssigneeItem>> groupAssigneeItems(List<TaskAssigneeVO> assignees) {

        Map<Long, List<AssigneeItem>> result = new HashMap<>();

        for (TaskAssigneeVO assignee : assignees) {
            result.computeIfAbsent(assignee.getTaskId(), key -> new ArrayList<>()).add(AssigneeItem.from(assignee));
        }

        return result;
    }
}
