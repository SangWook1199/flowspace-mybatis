package com.flowspace.database.dto.response;

import com.flowspace.database.service.BlockDatabaseRowVO;

// @formatter:off

// 데이터베이스 행 응답 DTO (행 = 페이지, TITLE 컬럼은 페이지 제목 사용)
public record BlockDatabaseRowResponse(

    Long rowId,
    Long databaseId,
    Long pageId,
    Integer position

) {

    public static BlockDatabaseRowResponse from(BlockDatabaseRowVO row) {
        return new BlockDatabaseRowResponse(
            row.getRowId(),
            row.getDatabaseId(),
            row.getPageId(),
            row.getPosition()
        );
    }
}

// @formatter:on
