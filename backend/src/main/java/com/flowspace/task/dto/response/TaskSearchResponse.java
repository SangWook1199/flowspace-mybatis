package com.flowspace.task.dto.response;

import com.flowspace.entity.Task;
import com.flowspace.entity.TaskAssignee;
import com.flowspace.task.service.TaskVO;

import java.util.List;

// @formatter:off

// Task 검색 응답 DTO
public record TaskSearchResponse(

    Long taskId,
    String title,

    Long statusId,
    String statusName,

    List<AssigneeItem> assignees

) {

    public static TaskSearchResponse from(
        Task task,
        List<TaskAssignee> assignees
    ) {
        return new TaskSearchResponse(
            task.getTaskId(),
            task.getTitle(),
            task.getStatus().getStatusId(),
            task.getStatus().getName(),
            assignees.stream()
                .map(AssigneeItem::from)
                .toList()
        );
    }

    // TaskVO 기반 응답 생성
    public static TaskSearchResponse from(
        TaskVO task,
        List<AssigneeItem> assignees
    ) {
        return new TaskSearchResponse(
            task.getTaskId(),
            task.getTitle(),
            task.getStatusId(),
            task.getStatusName(),
            assignees
        );
    }
}

// @formatter:on