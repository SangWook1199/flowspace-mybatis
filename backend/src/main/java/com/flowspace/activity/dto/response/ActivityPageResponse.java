package com.flowspace.activity.dto.response;

import java.util.List;

// @formatter:off

// 활동 기록 한 페이지 응답 DTO (page는 0부터 시작)
public record ActivityPageResponse(

    List<ActivityResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext

) {
}

// @formatter:on
