package com.flowspace.database.dto.response;

import java.util.List;

import com.flowspace.common.enums.DatabaseViewType;

// @formatter:off

// 데이터베이스 전체 조회 응답 DTO (컬럼·옵션·행·셀)
public record DatabaseDetailResponse(

    Long databaseId,
    String title,
    DatabaseViewType viewType,
    List<BlockDatabaseColumnResponse> columns,
    List<RowData> rows

) {

    // 행 (셀은 컬럼 순서)
    public record RowData(
        Long rowId,
        Long pageId,
        Integer position,
        List<CellData> cells
    ) {}

    // 셀
    public record CellData(
        Long columnId,
        String value
    ) {}

}

// @formatter:on