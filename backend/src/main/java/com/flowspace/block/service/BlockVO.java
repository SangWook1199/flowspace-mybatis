package com.flowspace.block.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.flowspace.common.enums.BlockType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 블록 (등록·수정, sp_block_list·sp_block 조회 공용)
@Getter
@Setter
@NoArgsConstructor
public class BlockVO {

    private Long blockId;

    private Long pageId;

    // 상위 블록 (토글 안의 블록처럼 들여쓴 블록, 최상위면 null)
    private Long parentBlockId;

    // 업무 블록이 가리키는 작업·일정·스프린트 (종류에 맞는 하나만 사용)
    private Long taskId;

    private Long eventId;

    private Long sprintId;

    private Long imageFileId;

    // ↓ 조회 전용 (이미지 주소, 데이터베이스 블록의 데이터베이스 번호)
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

    // ↓ 조회 전용: 블록이 속한 페이지
    private Long workspaceId;

    private Boolean pageDeleted;
}
