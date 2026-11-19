package com.flowspace.database.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 데이터베이스 행
@Getter
@Setter
@NoArgsConstructor
public class BlockDatabaseRowVO {

    private Long rowId;

    private Long databaseId;

    // 행 페이지 (표 블록은 null)
    private Long pageId;

    private Integer position;

    private LocalDateTime createdAt;
}
