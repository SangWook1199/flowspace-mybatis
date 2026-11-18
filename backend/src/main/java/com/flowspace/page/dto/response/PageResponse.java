package com.flowspace.page.dto.response;

import com.flowspace.entity.Page;
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

    // 스프린트 회고에 연결된 페이지인지 (회고 페이지는 스프린트마다 자동으로 생겨서 화면이 따로 묶어 보여줘요)
    boolean retrospective

) {

    // MyBatis 조회 결과(PageVO)로 만들어요 (커버 주소·회고 여부는 프로시저가 조인해 와요)
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

    // 아직 JPA인 페이지 복제에서 써요 (복제를 바꾸면 지워요)
    public static PageResponse from(Page page, boolean retrospective) {
        return new PageResponse(
            page.getPageId(),
            page.getWorkspace().getWorkspaceId(),
            page.getParentPage() == null ? null : page.getParentPage().getPageId(),
            page.getTitle(),
            page.getIcon(),
            page.getCoverFile() == null ? null : page.getCoverFile().getFileUrl(),
            page.getPosition(),
            page.getCreatedBy().getUserId(),
            page.getCreatedAt(),
            page.getUpdatedAt(),
            page.getDeletedAt(),
            retrospective
        );
    }
}

// @formatter:on