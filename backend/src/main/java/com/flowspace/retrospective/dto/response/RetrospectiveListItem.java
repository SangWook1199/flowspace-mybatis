package com.flowspace.retrospective.dto.response;

import java.time.LocalDate;

import com.flowspace.entity.enums.SprintStatus;
import com.flowspace.retrospective.service.RetrospectiveVO;

// @formatter:off

// 회고 목록 항목 (회고가 없는 스프린트 포함)
public record RetrospectiveListItem(

    Long sprintId,
    String sprintName,
    SprintStatus sprintStatus,
    LocalDate startDate,
    LocalDate endDate,
    String goal,
    Long retrospectiveId,
    Long pageId,
    Integer completionRate,
    Integer completedTask,
    Integer incompleteTask,
    Integer totalTask,
    Integer participantCount,
    Integer actionItemCount

) {

    // 회고가 생성된 스프린트
    public static RetrospectiveListItem of(
        RetrospectiveVO row,
        RetrospectiveSummary summary,
        int actionItemCount
    ) {
        return new RetrospectiveListItem(
            row.getSprintId(),
            row.getSprintName(),
            row.getSprintStatus(),
            row.getStartDate(),
            row.getEndDate(),
            row.getGoal(),
            row.getRetrospectiveId(),
            row.getPageId(),
            summary.completionRate(),
            summary.completedTask(),
            summary.incompleteTask(),
            summary.totalTask(),
            summary.participants().size(),
            actionItemCount
        );
    }

    // 아직 회고가 없는 스프린트
    public static RetrospectiveListItem withoutRetrospective(RetrospectiveVO row) {
        return new RetrospectiveListItem(
            row.getSprintId(),
            row.getSprintName(),
            row.getSprintStatus(),
            row.getStartDate(),
            row.getEndDate(),
            row.getGoal(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}

// @formatter:on
