package com.flowspace.database.dto.response;

import com.flowspace.database.service.BlockDatabaseCellVO;

// @formatter:off

// 셀 응답 DTO
public record BlockDatabaseCellResponse(

    Long rowId,
    Long columnId,
    String value

) {

    public static BlockDatabaseCellResponse from(BlockDatabaseCellVO cell) {
        return new BlockDatabaseCellResponse(
            cell.getRowId(),
            cell.getColumnId(),
            cell.getValue()
        );
    }
}

// @formatter:on
