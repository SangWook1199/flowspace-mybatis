package com.flowspace.block.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.flowspace.block.service.BlockVO;
import com.flowspace.common.enums.BlockType;

// @formatter:off

// 블록 응답 DTO
public record BlockResponse(

    Long blockId,
    Long pageId,
    Long taskId,
    Long eventId,
    Long sprintId,
    Long databaseId,
    String imageUrl,
    Long parentBlockId,
    BlockType type,
    BigDecimal position,
    String content,
    Long createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt

) {

    // BlockVO 기반 응답 생성
    public static BlockResponse from(BlockVO block) {
        return new BlockResponse(
            block.getBlockId(),
            block.getPageId(),
            block.getTaskId(),
            block.getEventId(),
            block.getSprintId(),
            block.getDatabaseId(),
            block.getImageUrl(),
            block.getParentBlockId(),
            block.getType(),
            block.getPosition(),
            block.getContent(),
            block.getCreatedBy(),
            block.getCreatedAt(),
            block.getUpdatedAt()
        );
    }
}

// @formatter:on
