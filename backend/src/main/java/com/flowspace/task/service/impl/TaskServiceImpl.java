package com.flowspace.task.service.impl;

import com.flowspace.service.ActivityService;
import com.flowspace.service.FileService;
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
import com.flowspace.dto.comment.CommentResponse;
import com.flowspace.task.dto.request.SubTaskCreateRequest;
import com.flowspace.task.dto.request.SubTaskReorderRequest;
import com.flowspace.task.dto.response.SubTaskResponse;
import com.flowspace.task.dto.request.SubTaskUpdateRequest;
import com.flowspace.entity.Comment;
import com.flowspace.entity.File;
import com.flowspace.entity.Sprint;
import com.flowspace.entity.User;
import com.flowspace.entity.Workspace;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.entity.enums.NotificationType;
import com.flowspace.entity.enums.TaskStatusCategory;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.repository.CommentRepository;
import com.flowspace.repository.TaskAttachmentRepository;
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
    private final CommentRepository commentRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final FileService fileService;
    private final UserRepository userRepository;

    private final ActivityService activityService;
    private final NotificationService notificationService;

    // 작업 조회용 MyBatis Mapper (같은 패키지라 import가 없어요)
    private final TaskMapper taskMapper;

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
        // 상태 수를 순서로 써서 맨 오른쪽 열에 놓여요
        status.setPosition(taskMapper.selectTaskStatusCount(workspaceId));

        // 상태를 먼저 만들어 번호를 받고, 그 번호로 워크스페이스에 연결해요
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

        // 기본 상태(1,2,3) 수정 → 공용 기본 상태는 그대로 두고, 이 워크스페이스 전용 새 상태로 바꿔 끼운다
        if (status.getIsDefault()) {

            TaskStatusVO newStatus = new TaskStatusVO();
            newStatus.setName(request.name());
            newStatus.setCategory(request.category());
            newStatus.setColor(request.color());

            taskMapper.insertTaskStatus(newStatus);

            // 같은 자리에 연결, 작업 이동, 기존 연결 끊기를 프로시저 한 번으로 해요
            taskMapper.updateDefaultTaskStatus(workspaceId, statusId, newStatus.getStatusId(), request.wipLimit());

            return TaskStatusResponse.from(findTaskStatus(workspaceId, newStatus.getStatusId()));
        }

        // 커스텀 상태 수정: 상태 정보와 워크스페이스별 WIP 제한은 테이블이 달라서 UPDATE가 두 번이에요
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

            // 따로 조회하지 않고, 바뀐 행 수로 그 워크스페이스의 상태인지 확인해요
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

        // 상태(컬럼) 삭제는 워크스페이스 멤버라면 누구나 할 수 있어요(삭제할 상태의 작업은 다른 상태로 옮겨져요).
        validateWorkspaceMember(workspaceId, user);

        // 마지막 남은 상태는 삭제할 수 없고, 작업을 옮길 대상이 삭제하는 상태 자신일 수도 없다
        if (taskMapper.selectTaskStatusCount(workspaceId) <= 1) {
            throw new FlowSpaceException(ErrorCode.LAST_TASK_STATUS_CANNOT_DELETE);
        }

        if (statusId.equals(request.targetStatusId())) {
            throw new FlowSpaceException(ErrorCode.INVALID_TASK_STATUS);
        }

        // 옮길 상태도 같은 워크스페이스의 상태여야 해요
        findTaskStatus(workspaceId, request.targetStatusId());

        // 작업 이동, 연결 끊기, 전용 상태 삭제를 프로시저 한 번으로 해요
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

    // Backlog Task 생성 (스프린트 없이 워크스페이스에 바로 만든다)
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

        // 같은 상태인 작업 수를 순서로 써서, 새 작업이 그 상태의 맨 뒤에 놓여요
        BigDecimal position = BigDecimal
            .valueOf(taskMapper.selectTaskCountByStatus(workspace.getWorkspaceId(), status.getStatusId()));

        // 작업 번호는 워크스페이스 기준으로 다음 번호를 줘요(스프린트와 상관없이 T-1, T-2 … 로 이어져요).
        int taskNumber = taskMapper.selectMaxTaskNumber(workspace.getWorkspaceId()) + 1;

        // 등록할 값을 VO에 담아요. 워크스페이스·스프린트·상태·작성자는 객체가 아니라 번호로 넣어요.
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

        // INSERT가 끝나면 DB가 만든 번호가 task.getTaskId()에 들어 있어요
        activityService.log(workspace, user, ActivityType.TASK_CREATED, ActivityTargetType.TASK, task.getTaskId());

        List<Long> assigneeIds = request.assigneeIds() == null ? List.of() : request.assigneeIds();

        for (Long assigneeId : assigneeIds) {

            User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

            workspaceMemberRepository.findByWorkspaceAndUser(workspace, assignee)
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

            taskMapper.insertTaskAssignee(task.getTaskId(), assignee.getUserId());

            notifyAssigned(task, workspace, assignee, user);
        }

        // 상태 이름과 만든 시각은 DB에만 있어서 다시 조회해서 돌려줘요
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

        // VO에는 워크스페이스 번호만 있어서, 멤버 확인에 쓸 워크스페이스를 따로 조회해요
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

        // 옮겨 갈 스프린트가 같은 워크스페이스인지 확인해요
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

        // 스프린트나 상태가 바뀌면 새 상태의 맨 뒤로 보내요
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

        // JPA는 값만 바꾸면 끝날 때 알아서 UPDATE 했지만, MyBatis는 직접 호출해야 DB에 반영돼요
        taskMapper.updateTask(task);

        // 새로 담당자가 된 사람에게만 알리려고, 바꾸기 전 담당자를 기억해둬요.
        Set<Long> previousAssigneeIds = Set.copyOf(taskMapper.selectTaskAssigneeUserIds(taskId));

        // 담당자는 모두 지우고 요청대로 다시 넣어요.
        // JPA 때는 지운 것을 먼저 DB에 보내려고 flush()가 필요했지만, MyBatis는 호출하는 순간 실행돼서 필요 없어요.
        taskMapper.deleteTaskAssignees(taskId);

        List<Long> assigneeIds = request.assigneeIds() == null ? List.of() : request.assigneeIds();

        // 원래부터 담당자였던 사람 (상태 변경 알림을 받을 사람)
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

        // 하위 작업 담당자는 작업 담당자 중에서만 고를 수 있어서, 작업 담당자에서 빠진 사람은 하위 작업에서도 비워요.
        // 하위 작업마다 반복하던 것을 UPDATE 한 번으로 해요.
        taskMapper.updateSubTaskAssigneeClear(taskId, assigneeIds);

        // 상태가 바뀌었으면 (이번에 새로 담당자가 된 사람을 뺀) 담당자들에게 알려요.
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

        // 스프린트 번호를 null로 넘기면 백로그 작업을 읽어요
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

        // 같은 Status면 변경하지 않음
        if (task.getStatusId().equals(request.statusId())) {
            return toTaskResponse(task);
        }

        // 새 상태의 맨 뒤로 보내요
        task.setPosition(BigDecimal
            .valueOf(taskMapper.selectTaskCountByStatus(task.getWorkspaceId(), status.getStatusId())));
        task.setStatusId(status.getStatusId());
        syncCompletedAt(task, status);

        taskMapper.updateTask(task);

        if (status.getCategory() == TaskStatusCategory.DONE) {
            activityService.log(workspace, user, ActivityType.TASK_COMPLETED, ActivityTargetType.TASK, taskId);
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

        // 관련 문서로 올려 둔 파일은 서버에서도 같이 지워야 해서, 작업을 지우기 전에 먼저 읽어 둬요.
        List<File> attachmentFiles = taskAttachmentRepository.findFilesByTaskId(taskId);

        // 작업 삭제와 같은 칸에 남은 작업의 순서 정리를 프로시저 한 번으로 해요
        taskMapper.deleteTask(taskId);

        for (File attachmentFile : attachmentFiles) {
            fileService.delete(attachmentFile);
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
        // 하위 작업 수를 순서로 써서 맨 뒤에 놓여요
        subTask.setPosition(taskMapper.selectSubTaskCount(taskId));

        taskMapper.insertSubTask(subTask);

        // 완료 여부는 DB 기본값으로 들어가서 다시 조회해서 돌려줘요
        return SubTaskResponse.from(findSubTask(subTask.getSubtaskId()));
    }

    // SubTask 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<SubTaskResponse> getSubTasks(Long taskId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        validateTaskMember(findTask(taskId), user);

        // 칸반 목록에서 쓰는 하위 작업 조회를 작업 하나로 불러요
        return taskMapper.selectSubTaskList(List.of(taskId)).stream().map(SubTaskResponse::from).toList();
    }

    // SubTask 수정
    @Override
    public SubTaskResponse updateSubTask(Long subTaskId, SubTaskUpdateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        SubTaskVO subTask = findSubTask(subTaskId);

        // 하위 작업에는 작업 번호만 있어서, 작업을 읽어 워크스페이스 멤버인지 확인해요
        validateTaskMember(findTask(subTask.getTaskId()), user);

        validateSubTaskAssignee(subTask.getTaskId(), request.assigneeId());

        subTask.setContent(request.content());
        subTask.setAssigneeId(request.assigneeId());
        subTask.setIsCompleted(request.isCompleted());

        taskMapper.updateSubTask(subTask);

        // DB가 채우는 값이 없어서 다시 조회하지 않고 VO로 바로 응답해요
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

        // 같은 곳으로 옮기면 바꾸지 않아요
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

        // 옮겨 간 곳에서 같은 상태의 맨 뒤로 가요
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

        // 화면이 계산한 순서를 작업마다 그대로 저장해요. 작업마다 확인할 것이 있어서 한 건씩 처리해요.
        for (TaskReorderRequest.Item item : request.tasks()) {

            TaskVO task = findTask(item.taskId());

            // 같은 워크스페이스는 JPA가 처음 한 번만 DB에서 읽고, 그다음부터는 기억해 둔 것을 써요
            Workspace workspace = workspaceRepository.findById(task.getWorkspaceId())
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

            workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

            TaskStatusVO status = validateWorkspaceStatus(workspace, item.statusId());

            // 칸반에서 다른 열로 옮긴 작업만(같은 열 안에서 순서만 바꾼 건 제외) 알려요.
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

    // 작업 상태가 바뀌었음을 담당자들에게 알림 (바꾼 본인은 제외돼요)
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

    // 작업에 쓸 상태가 이 워크스페이스의 상태인지 확인해요 (아니면 INVALID_TASK_STATUS)
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

    // 워크스페이스 멤버인지 확인해요
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

        // LIKE에서 %와 _는 "아무 글자"라는 뜻이라, 입력한 글자 그대로 찾도록 앞에 \를 붙여요(JPA의 Containing이 자동으로 하던 일)
        String escaped = search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

        List<TaskVO> tasks = taskMapper.selectTaskSearchList(workspaceId, escaped);

        // 결과가 없으면 IN ()이 되어 SQL 오류가 나서 여기서 끝내요
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

    // 작업 목록을 응답으로 바꿔요. sprintId가 null이면 백로그예요.
    // 작업과 담당자는 프로시저로, 하위 작업은 XML SQL로, 댓글은 아직 JPA로 한 번씩 읽어서 작업별로 나눠 담아요.
    private List<TaskResponse> toTaskResponses(Long workspaceId, Long sprintId) {

        List<TaskVO> tasks = taskMapper.selectTaskList(workspaceId, sprintId);

        // 작업이 없으면 IN ()이 되어 SQL 오류가 나서 여기서 끝내요
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

        Map<Long, List<CommentResponse>> commentsByTask = getTaskCommentResponses(taskIds);

        return tasks.stream()
            .map(task -> TaskResponse.from(task, assigneesByTask.getOrDefault(task.getTaskId(), List.of()),
                subtasksByTask.getOrDefault(task.getTaskId(), List.of()),
                commentsByTask.getOrDefault(task.getTaskId(), List.of())))
            .toList();
    }

    // 작업 한 건 (없으면 예외)
    private TaskVO findTask(Long taskId) {

        // MyBatis는 결과가 없으면 null을 돌려줘서 직접 확인해요
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

    // 작업이 속한 워크스페이스의 멤버인지 확인하고, 그 워크스페이스를 돌려줘요
    private Workspace validateTaskMember(TaskVO task, User user) {

        Workspace workspace = workspaceRepository.findById(task.getWorkspaceId())
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        return workspace;
    }

    // 하위 작업 담당자는 작업 담당자 중에서만 고를 수 있어요 (담당자가 없으면 확인하지 않아요)
    private void validateSubTaskAssignee(Long taskId, Long assigneeId) {

        if (assigneeId == null) {
            return;
        }

        userRepository.findById(assigneeId).orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        if (taskMapper.selectTaskAssigneeCount(taskId, assigneeId) == 0) {
            throw new FlowSpaceException(ErrorCode.INVALID_SUBTASK_ASSIGNEE);
        }
    }

    // 작업 한 건을 응답으로 바꿔요 (목록과 같은 방식: 담당자는 프로시저, 하위 작업은 XML SQL, 댓글은 아직 JPA)
    private TaskResponse toTaskResponse(TaskVO task) {

        Long taskId = task.getTaskId();

        List<AssigneeItem> assignees = taskMapper.selectTaskAssigneeListByTaskId(taskId).stream()
            .map(AssigneeItem::from).toList();

        List<SubTaskResponse> subtasks = taskMapper.selectSubTaskList(List.of(taskId)).stream()
            .map(SubTaskResponse::from).toList();

        List<CommentResponse> comments = getTaskCommentResponses(List.of(taskId)).getOrDefault(taskId, List.of());

        return TaskResponse.from(task, assignees, subtasks, comments);
    }

    // 담당자 조회 결과를 작업 번호별로 나눠 담아요 (칸반 목록과 검색에서 같이 써요)
    private Map<Long, List<AssigneeItem>> groupAssigneeItems(List<TaskAssigneeVO> assignees) {

        Map<Long, List<AssigneeItem>> result = new HashMap<>();

        for (TaskAssigneeVO assignee : assignees) {
            result.computeIfAbsent(assignee.getTaskId(), key -> new ArrayList<>()).add(AssigneeItem.from(assignee));
        }

        return result;
    }

    // 여러 작업의 댓글(대댓글 포함)을 한 번에 읽어서 작업 id별로 나눠 담아요.
    private Map<Long, List<CommentResponse>> getTaskCommentResponses(List<Long> taskIds) {

        Map<Long, List<CommentResponse>> result = new HashMap<>();

        List<Comment> roots = commentRepository.findByTask_TaskIdInAndParentCommentIsNullOrderByCreatedAtAsc(taskIds);

        if (roots.isEmpty()) {
            return result;
        }

        Map<Long, List<CommentResponse>> repliesByParent = new HashMap<>();

        for (Comment reply : commentRepository.findByParentCommentInOrderByCreatedAtAsc(roots)) {
            repliesByParent.computeIfAbsent(reply.getParentComment().getCommentId(), key -> new ArrayList<>())
                .add(CommentResponse.from(reply, List.of()));
        }

        for (Comment root : roots) {
            result.computeIfAbsent(root.getTask().getTaskId(), key -> new ArrayList<>()).add(
                CommentResponse.from(root, repliesByParent.getOrDefault(root.getCommentId(), List.of())));
        }

        return result;
    }
}
