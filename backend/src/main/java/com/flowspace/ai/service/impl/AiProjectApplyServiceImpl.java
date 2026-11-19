package com.flowspace.ai.service.impl;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.activity.service.ActivityService;
import com.flowspace.ai.dto.request.AiDraft;
import com.flowspace.ai.dto.response.AiApplyResponse;
import com.flowspace.ai.service.AiProjectApplyService;
import com.flowspace.calendar.service.EventVO;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.entity.enums.SprintStatus;
import com.flowspace.entity.enums.TaskStatusCategory;
import com.flowspace.entity.enums.WorkspaceColor;
import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.sprint.service.SprintVO;
import com.flowspace.task.service.SubTaskVO;
import com.flowspace.task.service.TaskStatusVO;
import com.flowspace.task.service.TaskVO;
import com.flowspace.user.service.UserVO;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

// 사용자가 확인한 AI 초안을 스프린트·작업·이벤트로 한 번에 만들어요.
// 하나의 트랜잭션이라 중간에 실패하면 아무것도 남지 않아요.
// 작업 담당자에게 보내는 알림은 만든 개수만큼 쏟아지지 않게 보내지 않아요.
@Service
@RequiredArgsConstructor
@Transactional
public class AiProjectApplyServiceImpl implements AiProjectApplyService {

    // 스프린트마다 돌려가며 쓰는 색 (흰색 제외)
    private static final List<WorkspaceColor> SPRINT_COLORS = List.of(WorkspaceColor.BLUE, WorkspaceColor.GREEN,
        WorkspaceColor.ORANGE, WorkspaceColor.PURPLE, WorkspaceColor.PINK, WorkspaceColor.RED);

    // 초안 일정 시작·종료 시각
    private static final LocalTime EVENT_START = LocalTime.of(10, 0);
    private static final LocalTime EVENT_END = LocalTime.of(11, 0);

    private final WorkspaceService workspaceService;
    private final ActivityService activityService;
    private final AiDraftValidator validator;

    // AI 기능 SQL
    private final AiMapper aiMapper;

    // 초안 일괄 등록 (관리자 이상, 담당자는 워크스페이스 멤버만, 작업 번호·칸반 위치는 기존 작업 뒤부터)
    @Override
    public AiApplyResponse apply(Long workspaceId, AiDraft draft, String email) {

        UserVO user = findUser(email);

        if (!workspaceService.getWorkspace(workspaceId, email).role().isAtLeast(WorkspaceRole.ADMIN)) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }

        Set<Long> memberIds = new HashSet<>(aiMapper.selectWorkspaceUserIds(workspaceId));

        validator.validateForApply(draft, memberIds);

        Long statusId = findStartStatusId(workspaceId);

        int taskNumber = aiMapper.selectMaxTaskNumber(workspaceId);
        int position = aiMapper.selectTaskCountByStatus(workspaceId, statusId);

        List<Long> sprintIds = new ArrayList<>();
        int taskCount = 0;
        int index = 0;

        for (AiDraft.Sprint draftSprint : draft.sprints()) {

            SprintVO sprint = new SprintVO();
            sprint.setWorkspaceId(workspaceId);
            sprint.setCreatedBy(user.getUserId());
            sprint.setName(draftSprint.name());
            sprint.setGoal(draftSprint.goal());
            sprint.setColor(SPRINT_COLORS.get(index % SPRINT_COLORS.size()));
            sprint.setStartDate(draftSprint.startDate());
            sprint.setEndDate(draftSprint.endDate());
            sprint.setStatus(SprintStatus.PLANNING);

            aiMapper.insertSprint(sprint);

            sprintIds.add(sprint.getSprintId());
            index++;

            for (AiDraft.Task draftTask : draftSprint.tasks()) {

                taskNumber++;

                TaskVO task = new TaskVO();
                task.setWorkspaceId(workspaceId);
                task.setSprintId(sprint.getSprintId());
                task.setStatusId(statusId);
                task.setTaskNumber(taskNumber);
                task.setPosition(BigDecimal.valueOf(position));
                task.setCreatedBy(user.getUserId());
                task.setTitle(draftTask.title());
                task.setDescription(draftTask.description());
                task.setStartDate(draftTask.startDate());
                task.setEndDate(draftTask.endDate());
                task.setPriority(draftTask.priority());

                aiMapper.insertTask(task);

                position++;
                taskCount++;

                if (draftTask.assigneeUserId() != null) {
                    aiMapper.insertTaskAssignee(task.getTaskId(), draftTask.assigneeUserId());
                }

                insertSubtasks(task.getTaskId(), draftTask.subtasks());
            }

            activityService.log(workspaceId, user.getUserId(), ActivityType.SPRINT_CREATED, ActivityTargetType.SPRINT,
                sprint.getSprintId());
        }

        List<AiDraft.Event> draftEvents = draft.events() == null ? List.of() : draft.events();

        for (AiDraft.Event draftEvent : draftEvents) {

            EventVO event = new EventVO();
            event.setWorkspaceId(workspaceId);
            event.setCreatedBy(user.getUserId());
            event.setTitle(draftEvent.title());
            event.setColor(WorkspaceColor.PURPLE);
            event.setStartDatetime(draftEvent.date().atTime(EVENT_START));
            event.setEndDatetime(draftEvent.date().atTime(EVENT_END));

            aiMapper.insertEvent(event);
        }

        return new AiApplyResponse(sprintIds.size(), taskCount, draftEvents.size(), sprintIds);
    }

    // 하위 작업 등록 (적은 순서대로, 담당자 없이 미완료)
    private void insertSubtasks(Long taskId, List<String> contents) {

        if (contents == null) {
            return;
        }

        int position = 0;

        for (String content : contents) {

            SubTaskVO subTask = new SubTaskVO();
            subTask.setTaskId(taskId);
            subTask.setContent(content.strip());
            subTask.setPosition(position++);

            aiMapper.insertSubTask(subTask);
        }
    }

    // 새 작업 상태 (TODO 분류 중 첫 번째, 없으면 맨 앞 상태)
    private Long findStartStatusId(Long workspaceId) {

        List<TaskStatusVO> statuses = aiMapper.selectTaskStatusList(workspaceId);

        return statuses.stream().filter(status -> status.getCategory() == TaskStatusCategory.TODO).findFirst()
            .or(() -> statuses.stream().findFirst()).map(TaskStatusVO::getStatusId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.INVALID_TASK_STATUS));
    }

    // ---------- 조회 + 없으면 예외 ----------

    private UserVO findUser(String email) {

        UserVO user = aiMapper.selectUserByEmail(email);

        if (user == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }
}
