package com.flowspace.block.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.flowspace.block.service.BlockVO;
import com.flowspace.entity.enums.BlockType;

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

    // 프로시저 조회 결과로 만들어요 (이미지 주소·데이터베이스 번호가 조인으로 들어 있어요)
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
