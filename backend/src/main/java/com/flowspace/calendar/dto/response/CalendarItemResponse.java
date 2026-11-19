package com.flowspace.calendar.dto.response;

import com.flowspace.calendar.service.CalendarTaskVO;
import com.flowspace.calendar.service.EventVO;
import com.flowspace.entity.enums.WorkspaceColor;
import com.flowspace.task.dto.response.AssigneeItem;
import com.flowspace.task.service.TaskAssigneeVO;

import java.time.LocalDateTime;
import java.util.List;

// @formatter:off

// 캘린더 항목 (작업 TASK, 일정 EVENT 공통 형식)
public record CalendarItemResponse(

    Long id,
    String type,
    String title,
    String description,
    WorkspaceColor color,
    LocalDateTime startDatetime,
    LocalDateTime endDatetime,
    Long sprintId,
    Long statusId,
    List<AssigneeItem> assignees

) {

    // 작업 항목: 시작 0시, 종료 23시 59분, 색은 작업 상태 색
    public static CalendarItemResponse from(
        CalendarTaskVO task,
        List<TaskAssigneeVO> assignees
    ) {
        return new CalendarItemResponse(
            task.getTaskId(),
            "TASK",
            task.getTitle(),
            task.getDescription(),
            task.getStatusColor(),
            task.getStartDate().atStartOfDay(),
            task.getEndDate() == null ? null : task.getEndDate().atTime(23, 59),
            task.getSprintId(),
            task.getStatusId(),
            assignees.stream()
                .map(AssigneeItem::from)
                .toList()
        );
    }

    public static CalendarItemResponse from(EventVO event) {
        return new CalendarItemResponse(
            event.getEventId(),
            "EVENT",
            event.getTitle(),
            event.getDescription(),
            event.getColor(),
            event.getStartDatetime(),
            event.getEndDatetime(),
            null,
            null,
            List.of()
        );
    }
}

// @formatter:on
