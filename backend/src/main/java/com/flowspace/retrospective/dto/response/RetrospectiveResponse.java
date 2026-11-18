package com.flowspace.retrospective.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.flowspace.dto.page.PageDetailResponse;
import com.flowspace.retrospective.service.RetrospectiveVO;

// @formatter:off

// 회고 상세: 요약, 상태·작업 스냅샷, 회고 페이지 내용
public record RetrospectiveResponse(

    Long retrospectiveId,
    Long sprintId,
    String sprintName,
    LocalDate startDate,
    LocalDate endDate,
    Long pageId,
    RetrospectiveSummary summary,
    List<StatusSnapshotItem> statuses,
    List<TaskSnapshotItem> snapshots,
    PageDetailResponse page

) {

    public static RetrospectiveResponse from(
        RetrospectiveVO retrospective,
        RetrospectiveSummary summary,
        List<StatusSnapshotItem> statuses,
        List<TaskSnapshotItem> snapshots,
        PageDetailResponse page
    ) {
        return new RetrospectiveResponse(
            retrospective.getRetrospectiveId(),
            retrospective.getSprintId(),
            retrospective.getSprintName(),
            retrospective.getStartDate(),
            retrospective.getEndDate(),
            retrospective.getPageId(),
            summary,
            statuses,
            snapshots,
            page
        );
    }
}

// @formatter:on
