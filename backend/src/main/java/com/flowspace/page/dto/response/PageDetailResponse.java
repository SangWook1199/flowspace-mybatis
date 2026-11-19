package com.flowspace.page.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.flowspace.block.dto.response.BlockResponse;
import com.flowspace.comment.dto.response.CommentResponse;
import com.flowspace.page.service.PageVO;

// @formatter:off

// 페이지 상세 응답 DTO (페이지 정보 + 블록 + 하위 페이지)
public record PageDetailResponse(

    Long pageId,
    String title,
    String icon,
    String coverUrl,
    Long version,
    List<BlockItem> blocks,
    List<ChildPageItem> childPages

) {

    // 블록 (댓글 포함)
    public record BlockItem(

        Long blockId,
        String type,
        BigDecimal position,
        String content,
        Long taskId,
        Long eventId,
        Long sprintId,
        Long databaseId,
        String imageUrl,
        Long parentBlockId,
        List<CommentResponse> comments

    ) {

        public static BlockItem from(
            BlockResponse block,
            List<CommentResponse> comments
        ) {
            return new BlockItem(
                block.blockId(),
                block.type().name(),
                block.position(),
                block.content(),
                block.taskId(),
                block.eventId(),
                block.sprintId(),
                block.databaseId(),
                block.imageUrl(),
                block.parentBlockId(),
                comments
            );
        }
    }

    // 하위 페이지
    public record ChildPageItem(

        Long pageId,
        String title,
        String icon

    ) {

        public static ChildPageItem from(PageVO page) {
            return new ChildPageItem(
                page.getPageId(),
                page.getTitle(),
                page.getIcon()
            );
        }
    }

    public static PageDetailResponse from(
        PageVO page,
        List<BlockItem> blockItems,
        List<PageVO> childPages
    ) {
        return new PageDetailResponse(
            page.getPageId(),
            page.getTitle(),
            page.getIcon(),
            page.getCoverUrl(),
            page.getVersion(),
            blockItems,
            childPages.stream().map(ChildPageItem::from).toList()
        );
    }
}

// @formatter:on
