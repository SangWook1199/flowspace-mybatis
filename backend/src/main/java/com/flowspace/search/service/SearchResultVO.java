package com.flowspace.search.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 검색 결과 한 줄 (종류마다 쓰는 컬럼만 채움)
@Getter
@Setter
@NoArgsConstructor
public class SearchResultVO {

    // 페이지·블록·작업·스프린트·댓글·일정 번호
    private Long id;

    // 제목 (댓글은 작업 또는 페이지 이름)
    private String title;

    // 본문 (작업·일정 설명, 스프린트 목표, 댓글 내용, 블록 JSON)
    private String body;

    // 페이지 아이콘
    private String icon;

    // 보조 정보 (작업 상태 이름, 댓글 작성자 닉네임)
    private String meta;

    // 블록·댓글이 있는 페이지
    private Long pageId;

    // 댓글이 달린 작업
    private Long taskId;

    // 작업의 스프린트 (작업 링크용, 백로그면 null)
    private Long sprintId;

    // 일정 시작 시각
    private LocalDateTime startDatetime;
}
