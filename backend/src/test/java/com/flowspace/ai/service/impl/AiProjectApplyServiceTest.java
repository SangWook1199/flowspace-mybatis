package com.flowspace.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flowspace.activity.service.ActivityService;
import com.flowspace.ai.dto.request.AiDraft;
import com.flowspace.ai.dto.response.AiApplyResponse;
import com.flowspace.calendar.service.EventVO;
import com.flowspace.common.enums.ActivityTargetType;
import com.flowspace.common.enums.ActivityType;
import com.flowspace.common.enums.SprintStatus;
import com.flowspace.common.enums.TaskPriority;
import com.flowspace.common.enums.TaskStatusCategory;
import com.flowspace.common.enums.WorkspaceColor;
import com.flowspace.common.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.sprint.service.SprintVO;
import com.flowspace.task.service.SubTaskVO;
import com.flowspace.task.service.TaskStatusVO;
import com.flowspace.task.service.TaskVO;
import com.flowspace.user.service.UserVO;
import com.flowspace.workspace.dto.response.WorkspaceResponse;
import com.flowspace.workspace.service.WorkspaceService;

// AI 초안 만들기: 관리자만 쓸 수 있는지, 스프린트·작업·이벤트가 올바르게 이어서 만들어지는지,
// 잘못된 초안은 아무것도 저장하지 않는지 확인해요.
@ExtendWith(MockitoExtension.class)
class AiProjectApplyServiceTest {

    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private ActivityService activityService;
    @Mock
    private AiMapper aiMapper;
    @Spy
    private AiDraftValidator validator = new AiDraftValidator();

    @InjectMocks
    private AiProjectApplyServiceImpl service;

    private static final LocalDate START = LocalDate.of(2026, 10, 12);

    private final TaskStatusVO todo = status(10L, TaskStatusCategory.TODO);

    @BeforeEach
    void setUp() {
        UserVO me = new UserVO();
        me.setUserId(1L);
        me.setEmail("a@a.com");
        me.setNickname("a");

        when(aiMapper.selectUserByEmail("a@a.com")).thenReturn(me);
    }

    // 작업 상태 한 개
    private static TaskStatusVO status(Long statusId, TaskStatusCategory category) {
        TaskStatusVO status = new TaskStatusVO();
        status.setWorkspaceId(5L);
        status.setStatusId(statusId);
        status.setName(category.name());
        status.setCategory(category);
        return status;
    }

    // 로그인한 사람의 워크스페이스 역할
    private void myRole(WorkspaceRole role) {
        when(workspaceService.getWorkspace(5L, "a@a.com"))
            .thenReturn(new WorkspaceResponse(5L, "w", "W", null, null, 1L, role));
    }

    // 워크스페이스 멤버의 사용자 번호
    private void workspaceMembers(Long... userIds) {
        when(aiMapper.selectWorkspaceUserIds(5L)).thenReturn(List.of(userIds));
    }

    // 워크스페이스 작업 상태 (위치 순)
    private void statuses(TaskStatusVO... statuses) {
        when(aiMapper.selectTaskStatusList(5L)).thenReturn(List.of(statuses));
    }

    // 등록할 때 새 번호 채우기 (스프린트 100번부터, 작업 200번부터)
    private void fillIds() {
        AtomicLong sprintId = new AtomicLong(100);
        AtomicLong taskId = new AtomicLong(200);

        doAnswer(invocation -> {
            invocation.<SprintVO>getArgument(0).setSprintId(sprintId.getAndIncrement());
            return 1;
        }).when(aiMapper).insertSprint(any(SprintVO.class));

        doAnswer(invocation -> {
            invocation.<TaskVO>getArgument(0).setTaskId(taskId.getAndIncrement());
            return 1;
        }).when(aiMapper).insertTask(any(TaskVO.class));
    }

    private AiDraft.Task draftTask(String title, Long assignee, int dayOffset) {
        return new AiDraft.Task(title, "설명", TaskPriority.HIGH, assignee, START.plusDays(dayOffset),
            START.plusDays(dayOffset + 1));
    }

    // 스프린트 2개(작업 2개 + 1개), 일정 1개
    private AiDraft draft() {
        return new AiDraft(
            List.of(
                new AiDraft.Sprint("기획", "목표1", START, START.plusDays(13),
                    List.of(draftTask("정의서 쓰기", 1L, 0), draftTask("담당 없는 작업 하기", null, 1))),
                new AiDraft.Sprint("개발", null, START.plusDays(14), START.plusDays(27),
                    List.of(draftTask("API 구현하기", 2L, 14)))),
            List.of(new AiDraft.Event("스프린트 리뷰", START.plusDays(11))));
    }

    @Test
    @DisplayName("관리자는 초안으로 스프린트·작업·이벤트를 한 번에 만든다")
    void adminAppliesDraft() {
        myRole(WorkspaceRole.ADMIN);
        workspaceMembers(1L, 2L);
        statuses(todo);
        fillIds();

        when(aiMapper.selectMaxTaskNumber(5L)).thenReturn(7);
        when(aiMapper.selectTaskCountByStatus(5L, 10L)).thenReturn(0);

        AiApplyResponse response = service.apply(5L, draft(), "a@a.com");

        assertEquals(2, response.sprintCount());
        assertEquals(3, response.taskCount());
        assertEquals(1, response.eventCount());
        assertEquals(List.of(100L, 101L), response.sprintIds());

        ArgumentCaptor<SprintVO> sprints = ArgumentCaptor.forClass(SprintVO.class);
        verify(aiMapper, times(2)).insertSprint(sprints.capture());
        assertEquals("기획", sprints.getAllValues().get(0).getName());
        assertEquals(SprintStatus.PLANNING, sprints.getAllValues().get(0).getStatus());
        assertEquals(WorkspaceColor.BLUE, sprints.getAllValues().get(0).getColor());
        assertEquals(WorkspaceColor.GREEN, sprints.getAllValues().get(1).getColor());
        assertEquals(List.of(1L, 1L), sprints.getAllValues().stream().map(SprintVO::getCreatedBy).toList());

        ArgumentCaptor<TaskVO> tasks = ArgumentCaptor.forClass(TaskVO.class);
        verify(aiMapper, times(3)).insertTask(tasks.capture());
        List<TaskVO> savedTasks = tasks.getAllValues();
        assertEquals(List.of(8, 9, 10), savedTasks.stream().map(TaskVO::getTaskNumber).toList());
        assertEquals(List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(1), BigDecimal.valueOf(2)),
            savedTasks.stream().map(TaskVO::getPosition).toList());
        assertEquals(List.of(10L, 10L, 10L), savedTasks.stream().map(TaskVO::getStatusId).toList());
        assertEquals(List.of(100L, 100L, 101L), savedTasks.stream().map(TaskVO::getSprintId).toList());
        assertEquals(TaskPriority.HIGH, savedTasks.get(0).getPriority());

        verify(aiMapper, times(2)).insertTaskAssignee(anyLong(), anyLong());
        verify(aiMapper).insertTaskAssignee(200L, 1L);
        verify(aiMapper).insertTaskAssignee(202L, 2L);

        ArgumentCaptor<EventVO> events = ArgumentCaptor.forClass(EventVO.class);
        verify(aiMapper).insertEvent(events.capture());
        assertEquals("스프린트 리뷰", events.getValue().getTitle());
        assertEquals(LocalDateTime.of(2026, 10, 23, 10, 0), events.getValue().getStartDatetime());
        assertEquals(LocalDateTime.of(2026, 10, 23, 11, 0), events.getValue().getEndDatetime());

        verify(activityService).log(eq(5L), eq(1L), eq(ActivityType.SPRINT_CREATED), eq(ActivityTargetType.SPRINT),
            eq(100L));
        verify(activityService).log(eq(5L), eq(1L), eq(ActivityType.SPRINT_CREATED), eq(ActivityTargetType.SPRINT),
            eq(101L));
    }

    @Test
    @DisplayName("할 일(TODO) 분류가 있으면 앞에 다른 상태가 있어도 TODO에 놓는다")
    void prefersTodoCategory() {
        myRole(WorkspaceRole.OWNER);
        workspaceMembers(1L, 2L);
        statuses(status(11L, TaskStatusCategory.IN_PROGRESS), todo);

        when(aiMapper.selectMaxTaskNumber(5L)).thenReturn(0);
        when(aiMapper.selectTaskCountByStatus(5L, 10L)).thenReturn(0);

        service.apply(5L, draft(), "a@a.com");

        ArgumentCaptor<TaskVO> tasks = ArgumentCaptor.forClass(TaskVO.class);
        verify(aiMapper, times(3)).insertTask(tasks.capture());
        assertEquals(List.of(10L, 10L, 10L), tasks.getAllValues().stream().map(TaskVO::getStatusId).toList());
    }

    @Test
    @DisplayName("일반 멤버는 AI 초안을 만들 수 없다")
    void memberCannotApply() {
        myRole(WorkspaceRole.MEMBER);

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> service.apply(5L, draft(), "a@a.com"));

        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(aiMapper, never()).insertSprint(any());
        verify(aiMapper, never()).insertTask(any());
    }

    @Test
    @DisplayName("워크스페이스 멤버가 아니면 만들 수 없다")
    void nonMemberCannotApply() {
        when(workspaceService.getWorkspace(5L, "a@a.com")).thenThrow(new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> service.apply(5L, draft(), "a@a.com"));

        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(aiMapper, never()).insertSprint(any());
    }

    @Test
    @DisplayName("멤버가 아닌 사람을 담당자로 지정한 초안은 아무것도 만들지 않는다")
    void outsiderAssigneeSavesNothing() {
        myRole(WorkspaceRole.ADMIN);
        workspaceMembers(1L);

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> service.apply(5L, draft(), "a@a.com"));

        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(aiMapper, never()).insertSprint(any());
        verify(aiMapper, never()).insertTask(any());
        verify(aiMapper, never()).insertEvent(any());
    }

    @Test
    @DisplayName("작업 상태가 하나도 없으면 만들지 않는다")
    void noStatusSavesNothing() {
        myRole(WorkspaceRole.ADMIN);
        workspaceMembers(1L, 2L);
        statuses();

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> service.apply(5L, draft(), "a@a.com"));

        assertEquals(ErrorCode.INVALID_TASK_STATUS, e.getErrorCode());
        verify(aiMapper, never()).insertSprint(any());
    }

    @Test
    @DisplayName("작업의 하위 작업은 적은 순서대로, 담당자 없이 저장한다")
    void savesSubtasksInOrder() {
        myRole(WorkspaceRole.ADMIN);
        workspaceMembers(1L, 2L);
        statuses(todo);
        fillIds();

        when(aiMapper.selectMaxTaskNumber(5L)).thenReturn(0);
        when(aiMapper.selectTaskCountByStatus(5L, 10L)).thenReturn(0);

        AiDraft draft = new AiDraft(
            List.of(new AiDraft.Sprint("개발", null, START, START.plusDays(13),
                List.of(new AiDraft.Task("회원가입 화면 구현", "설명", TaskPriority.HIGH, 1L, START, START.plusDays(1),
                    List.of("회원가입 화면 구성", " 회원가입 API 연결 ")),
                    draftTask("하위 작업 없는 작업", null, 3)))),
            List.of());

        service.apply(5L, draft, "a@a.com");

        ArgumentCaptor<SubTaskVO> saved = ArgumentCaptor.forClass(SubTaskVO.class);
        verify(aiMapper, times(2)).insertSubTask(saved.capture());

        List<SubTaskVO> subtasks = saved.getAllValues();
        assertEquals("회원가입 화면 구성", subtasks.get(0).getContent());
        assertEquals("회원가입 API 연결", subtasks.get(1).getContent());
        assertEquals(List.of(0, 1), subtasks.stream().map(SubTaskVO::getPosition).toList());
        assertEquals(List.of(200L, 200L), subtasks.stream().map(SubTaskVO::getTaskId).toList());
        assertNull(subtasks.get(0).getAssigneeId());
    }
}
