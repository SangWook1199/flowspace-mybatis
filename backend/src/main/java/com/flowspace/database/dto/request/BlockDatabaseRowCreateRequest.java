package com.flowspace.database.dto.request;

// @formatter:off

// 데이터베이스 행 생성 요청 DTO (본문은 생략 가능)
public record BlockDatabaseRowCreateRequest(

    // 연결할 기존 페이지 (없으면 새 페이지 생성)
    Long pageId

) {
}

// @formatter:on
