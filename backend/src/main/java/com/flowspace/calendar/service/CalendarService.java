package com.flowspace.calendar.service;

import java.util.List;

import com.flowspace.calendar.dto.response.CalendarItemResponse;

// CalendarServiceImpl가 실제 동작을 맡아요. Controller와 다른 서비스는 이 인터페이스만 알아요.
public interface CalendarService {

    // 캘린더 조회
    List<CalendarItemResponse> getCalendar(Long workspaceId, Integer year, Integer month, Long sprintId, String email);
}
