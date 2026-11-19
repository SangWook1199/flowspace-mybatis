package com.flowspace.database.service;

import java.time.LocalDateTime;
import com.flowspace.entity.enums.DatabaseViewType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 데이터베이스 블록 (등록, sp_block_database 조회)
@Getter
@Setter
@NoArgsConstructor
public class BlockDatabaseVO {

    private Long databaseId;

    private Long blockId;

    private String title;

    private DatabaseViewType viewType;

    private LocalDateTime createdAt;

    // 블록이 있는 페이지 (조회 전용)
    private Long pageId;

    private Long workspaceId;

    private Boolean pageDeleted;
}
