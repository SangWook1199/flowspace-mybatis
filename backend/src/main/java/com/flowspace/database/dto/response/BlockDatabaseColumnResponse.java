package com.flowspace.database.dto.response;

import java.util.List;

import com.flowspace.database.service.BlockDatabaseColumnVO;
import com.flowspace.common.enums.DatabaseColumnType;

// @formatter:off

// 데이터베이스 컬럼 응답 DTO
public record BlockDatabaseColumnResponse(

    Long columnId,
    Long databaseId,
    String name,
    DatabaseColumnType type,
    Integer position,
    Integer width,
    List<BlockDatabaseColumnOptionResponse> options

) {

    // 컬럼 + 옵션 목록 (옵션 없는 컬럼은 빈 목록)
    public static BlockDatabaseColumnResponse from(BlockDatabaseColumnVO column,
        List<BlockDatabaseColumnOptionResponse> options) {
        return new BlockDatabaseColumnResponse(
            column.getColumnId(),
            column.getDatabaseId(),
            column.getName(),
            column.getType(),
            column.getPosition(),
            column.getWidth(),
            options
        );
    }
}

// @formatter:on
