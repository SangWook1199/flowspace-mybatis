package com.flowspace.database.dto.response;

import com.flowspace.database.service.BlockDatabaseVO;
import com.flowspace.entity.enums.DatabaseViewType;

// @formatter:off

// 데이터베이스 응답 DTO
public record DatabaseResponse(

    Long databaseId,
    Long blockId,
    String name,
    DatabaseViewType viewType

) {

    public static DatabaseResponse from(BlockDatabaseVO database) {
        return new DatabaseResponse(
            database.getDatabaseId(),
            database.getBlockId(),
            database.getTitle(),
            database.getViewType()
        );
    }
}

// @formatter:on
