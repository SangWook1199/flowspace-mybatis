package com.flowspace.calendar.service;

import java.time.LocalDateTime;

import com.flowspace.entity.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 일정 (등록·수정, sp_event·월별 목록·검색 조회 공용)
@Getter
@Setter
@NoArgsConstructor
public class EventVO {

    private Long eventId;

    private Long workspaceId;

    // 만든 사람 번호
    private Long createdBy;

    // 작성자 이름 (sp_event 조회 전용)
    private String createdByName;

    private String title;

    private String description;

    private WorkspaceColor color;

    private LocalDateTime startDatetime;

    // 종료 시각 (없으면 null)
    private LocalDateTime endDatetime;

    private LocalDateTime createdAt;
}
