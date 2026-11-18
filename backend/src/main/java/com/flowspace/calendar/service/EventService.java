package com.flowspace.calendar.service;

import java.util.List;

import com.flowspace.calendar.dto.request.EventCreateRequest;
import com.flowspace.calendar.dto.request.EventUpdateRequest;
import com.flowspace.calendar.dto.response.EventResponse;
import com.flowspace.calendar.dto.response.EventSearchResponse;

// EventServiceImpl가 실제 동작을 맡아요. Controller와 다른 서비스는 이 인터페이스만 알아요.
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
