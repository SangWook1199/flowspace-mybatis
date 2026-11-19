package com.flowspace.task.dto.response;

import com.flowspace.comment.dto.response.CommentResponse;
import com.flowspace.common.enums.TaskPriority;
import com.flowspace.task.service.TaskVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// @formatter:off

// Task 응답 DTO
public record TaskResponse(

    Long taskId,
    Integer taskNumber,
    Long workspaceId,
    Long sprintId,

    Long statusId,
    String statusName,

    BigDecimal position,

    List<AssigneeItem> assignees,

    String title,
    String description,

    LocalDate startDate,
    LocalDate endDate,

    TaskPriority priority,

    LocalDateTime completedAt,
    LocalDateTime createdAt,

    List<SubTaskResponse> subtasks,
    List<CommentResponse> comments

) {

    // TaskVO 기반 응답 생성
    public static TaskResponse from(
        TaskVO task,
        List<AssigneeItem> assignees,
        List<SubTaskResponse> subtasks,
        List<CommentResponse> comments
    ) {
        return new TaskResponse(
            task.getTaskId(),
            task.getTaskNumber(),
            task.getWorkspaceId(),
            task.getSprintId(),
            task.getStatusId(),
            task.getStatusName(),
            task.getPosition(),
            assignees,
            task.getTitle(),
            task.getDescription(),
            task.getStartDate(),
            task.getEndDate(),
            task.getPriority(),
            task.getCompletedAt(),
            task.getCreatedAt(),
            subtasks,
            comments
        );
    }
}

// @formatter:on