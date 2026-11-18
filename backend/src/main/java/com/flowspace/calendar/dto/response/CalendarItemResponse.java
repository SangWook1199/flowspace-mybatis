package com.flowspace.calendar.dto.response;

import com.flowspace.calendar.service.CalendarTaskVO;
import com.flowspace.calendar.service.EventVO;
import com.flowspace.entity.enums.WorkspaceColor;
import com.flowspace.task.dto.response.AssigneeItem;
import com.flowspace.task.service.TaskAssigneeVO;

import java.time.LocalDateTime;
import java.util.List;

// @formatter:off

// 캘린더 한 칸. 작업(TASK)과 일정(EVENT)을 같은 모양으로 내려줘요.
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

    // 작업: 날짜만 있어서 시작은 0시, 끝은 23시 59분으로 바꿔요. 색은 작업 상태의 색이에요.
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
