package com.flowspace.block.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.flowspace.entity.enums.BlockType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 블록 한 개예요. 등록·수정할 때와 조회할 때(sp_block_list, sp_block) 같이 써요.
@Getter
@Setter
@NoArgsConstructor
public class BlockVO {

    private Long blockId;

    private Long pageId;

    // 상위 블록 (토글 안의 블록처럼 들여쓴 블록, 최상위면 null)
    private Long parentBlockId;

    // 업무 블록이 가리키는 작업·일정·스프린트 (종류에 맞는 하나만 들어 있어요)
    private Long taskId;

    private Long eventId;

    private Long sprintId;

    private Long imageFileId;

    // ↓ 조회할 때만 채워져요 (이미지 주소, 데이터베이스 블록이면 데이터베이스 번호)
    private String imageUrl;

    private Long databaseId;

    private BlockType type;

    // 페이지 안의 순서
    private BigDecimal position;

    // 블록 내용 (JSON 문자열)
    private String content;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ↓ 조회할 때만: 블록이 있는 페이지
    private Long workspaceId;

    private Boolean pageDeleted;
}
