package com.flowspace.calendar.service;

import java.util.List;

import com.flowspace.calendar.dto.response.CalendarItemResponse;

// 캘린더 서비스
public interface CalendarService {

    // 캘린더 조회
    List<CalendarItemResponse> getCalendar(Long workspaceId, Integer year, Integer month, Long sprintId, String email);
}
