package com.flowspace.page.dto.response;

import com.flowspace.page.service.PageVO;

import java.time.LocalDateTime;

// @formatter:off

// 페이지 응답 DTO
public record PageResponse(

    Long pageId,
    Long workspaceId,
    Long parentPageId,
    String title,
    String icon,
    String coverUrl,
    Integer position,
    Long createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt,

    // 스프린트 회고 페이지 여부 (화면에서 따로 묶어 표시)
    boolean retrospective

) {

    // PageVO 기반 응답 생성
    public static PageResponse from(PageVO page) {
        return new PageResponse(
            page.getPageId(),
            page.getWorkspaceId(),
            page.getParentPageId(),
            page.getTitle(),
            page.getIcon(),
            page.getCoverUrl(),
            page.getPosition(),
            page.getCreatedBy(),
            page.getCreatedAt(),
            page.getUpdatedAt(),
            page.getDeletedAt(),
            Boolean.TRUE.equals(page.getRetrospective())
        );
    }
}

// @formatter:on