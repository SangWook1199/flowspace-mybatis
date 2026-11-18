package com.flowspace.calendar.dto.response;

import java.time.LocalDateTime;

import com.flowspace.calendar.service.EventVO;

// @formatter:off

// Event 검색 응답 DTO
public record EventSearchResponse(

    Long eventId,
    String title,
    LocalDateTime startAt,
    LocalDateTime endAt

) {

    public static EventSearchResponse from(EventVO event) {
        return new EventSearchResponse(
            event.getEventId(),
            event.getTitle(),
            event.getStartDatetime(),
            event.getEndDatetime()
        );
    }
}

// @formatter:on
