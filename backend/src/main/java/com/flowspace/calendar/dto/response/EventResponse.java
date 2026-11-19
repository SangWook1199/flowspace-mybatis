package com.flowspace.calendar.dto.response;

import com.flowspace.calendar.service.EventVO;
import com.flowspace.entity.enums.WorkspaceColor;

import java.time.LocalDateTime;

// @formatter:off

public record EventResponse(

    Long eventId,
    Long workspaceId,
    String title,
    String description,
    WorkspaceColor color,
    LocalDateTime startDatetime,
    LocalDateTime endDatetime,
    String createdByName,
    LocalDateTime createdAt

) {

    // sp_event 조회 결과 기반 응답 생성
    public static EventResponse from(EventVO event) {
        return new EventResponse(
            event.getEventId(),
            event.getWorkspaceId(),
            event.getTitle(),
            event.getDescription(),
            event.getColor(),
            event.getStartDatetime(),
            event.getEndDatetime(),
            event.getCreatedByName(),
            event.getCreatedAt()
        );
    }
}

// @formatter:on
