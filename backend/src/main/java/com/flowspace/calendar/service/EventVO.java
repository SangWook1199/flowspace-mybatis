package com.flowspace.calendar.service;

import java.time.LocalDateTime;

import com.flowspace.entity.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 일정 한 건이에요. 등록·수정할 때와 조회할 때(sp_event, 한 달 일정 목록, 검색) 같이 써요.
@Getter
@Setter
@NoArgsConstructor
public class EventVO {

    private Long eventId;

    private Long workspaceId;

    // 만든 사람 번호
    private Long createdBy;

    // 만든 사람 이름 (sp_event로 조회할 때만 채워져요)
    private String createdByName;

    private String title;

    private String description;

    private WorkspaceColor color;

    private LocalDateTime startDatetime;

    // 끝나는 시각 (없을 수 있어요)
    private LocalDateTime endDatetime;

    private LocalDateTime createdAt;
}
