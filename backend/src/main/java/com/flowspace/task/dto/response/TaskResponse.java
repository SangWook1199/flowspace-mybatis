package com.flowspace.task.dto.response;

import com.flowspace.comment.dto.response.CommentResponse;
import com.flowspace.entity.SubTask;
import com.flowspace.entity.Task;
import com.flowspace.entity.TaskAssignee;
import com.flowspace.entity.enums.TaskPriority;
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

    public static TaskResponse from(
        Task task,
        List<TaskAssignee> assignees,
        List<SubTask> subtasks,
        List<CommentResponse> comments
    ) {
        return new TaskResponse(
            task.getTaskId(),
            task.getTaskNumber(),
            task.getWorkspace().getWorkspaceId(),
            task.getSprint() == null ? null : task.getSprint().getSprintId(),
            task.getStatus().getStatusId(),
            task.getStatus().getName(),
            task.getPosition(),
            assignees.stream()
                .map(AssigneeItem::from)
                .toList(),
            task.getTitle(),
            task.getDescription(),
            task.getStartDate(),
            task.getEndDate(),
            task.getPriority(),
            task.getCompletedAt(),
            task.getCreatedAt(),
            subtasks.stream()
                .map(SubTaskResponse::from)
                .toList(),
            comments
        );
    }

    // MyBatis 조회 결과(TaskVO)로 응답을 만들어요
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