package com.flowspace.database.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 데이터베이스 셀 (행 + 컬럼)
@Getter
@Setter
@NoArgsConstructor
public class BlockDatabaseCellVO {

    private Long rowId;

    private Long columnId;

    private String value;
}
