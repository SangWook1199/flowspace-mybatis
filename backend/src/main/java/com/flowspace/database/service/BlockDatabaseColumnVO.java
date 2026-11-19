package com.flowspace.database.service;

import java.time.LocalDateTime;
import com.flowspace.entity.enums.DatabaseColumnType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 데이터베이스 컬럼
@Getter
@Setter
@NoArgsConstructor
public class BlockDatabaseColumnVO {

    private Long columnId;

    private Long databaseId;

    private String name;

    private DatabaseColumnType type;

    private Integer position;

    // 너비 (px, 기본 200)
    private Integer width;

    private LocalDateTime createdAt;
}
