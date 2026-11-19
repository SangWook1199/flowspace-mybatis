package com.flowspace.calendar.service;

import java.util.List;

import com.flowspace.calendar.dto.request.EventCreateRequest;
import com.flowspace.calendar.dto.request.EventUpdateRequest;
import com.flowspace.calendar.dto.response.EventResponse;
import com.flowspace.calendar.dto.response.EventSearchResponse;

// 일정 서비스
public interface EventService {

    // 이벤트 생성
    EventResponse createEvent(Long workspaceId, EventCreateRequest request, String email);

    // 이벤트 단건 조회
    EventResponse getEvent(Long eventId, String email);

    // 이벤트 수정
    EventResponse updateEvent(Long eventId, EventUpdateRequest request, String email);

    // 이벤트 삭제
    void deleteEvent(Long eventId, String email);

    // Event 검색
    List<EventSearchResponse> searchEvents(Long workspaceId, String keyword, String email);
}
