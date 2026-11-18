package com.flowspace.calendar.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.calendar.dto.request.EventCreateRequest;
import com.flowspace.calendar.dto.request.EventUpdateRequest;
import com.flowspace.calendar.dto.response.EventResponse;
import com.flowspace.calendar.dto.response.EventSearchResponse;
import com.flowspace.calendar.service.EventService;
import com.flowspace.calendar.service.EventVO;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    // 캘린더·일정 SQL (같은 패키지라 import가 없어요)
    private final CalendarMapper calendarMapper;
    private final WorkspaceService workspaceService;

    // 이벤트 생성
    @Override
    public EventResponse createEvent(Long workspaceId, EventCreateRequest request, String email) {

        // 워크스페이스가 있는지, 만드는 사람이 멤버인지는 워크스페이스 서비스의 규칙을 그대로 써요
        workspaceService.getWorkspace(workspaceId, email);
        validateEventTime(request.startDatetime(), request.endDatetime());

        EventVO event = new EventVO();
        event.setWorkspaceId(workspaceId);
        event.setCreatedBy(calendarMapper.selectUserIdByEmail(email));
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setColor(request.color());
        event.setStartDatetime(request.startDatetime());
        event.setEndDatetime(request.endDatetime());

        calendarMapper.insertEvent(event);

        // 만든 사람 이름과 만든 시각까지 담으려고 다시 읽어요
        return EventResponse.from(findEvent(event.getEventId()));
    }

    // 이벤트 단건 조회
    @Transactional(readOnly = true)
    @Override
    public EventResponse getEvent(Long eventId, String email) {

        EventVO event = findEvent(eventId);
        workspaceService.getWorkspace(event.getWorkspaceId(), email);

        return EventResponse.from(event);
    }

    // 이벤트 수정
    @Override
    public EventResponse updateEvent(Long eventId, EventUpdateRequest request, String email) {

        EventVO event = findEvent(eventId);
        workspaceService.getWorkspace(event.getWorkspaceId(), email);
        validateEventTime(request.startDatetime(), request.endDatetime());

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setColor(request.color());
        event.setStartDatetime(request.startDatetime());
        event.setEndDatetime(request.endDatetime());

        calendarMapper.updateEvent(event);

        return EventResponse.from(event);
    }

    // 이벤트 삭제
    @Override
    public void deleteEvent(Long eventId, String email) {

        EventVO event = findEvent(eventId);
        workspaceService.getWorkspace(event.getWorkspaceId(), email);

        calendarMapper.deleteEvent(eventId);
    }

    // Event 검색
    @Transactional(readOnly = true)
    @Override
    public List<EventSearchResponse> searchEvents(Long workspaceId, String keyword, String email) {

        workspaceService.getWorkspace(workspaceId, email);

        String search = keyword == null ? "" : keyword;

        // LIKE에서 %와 _는 "아무 글자"라는 뜻이라, 입력한 글자 그대로 찾도록 앞에 \를 붙여요(작업 검색과 같아요)
        String escaped = search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

        return calendarMapper.selectEventSearchList(workspaceId, escaped).stream()
            .map(EventSearchResponse::from).toList();
    }

    // 일정 한 건 (없으면 예외)
    private EventVO findEvent(Long eventId) {

        EventVO event = calendarMapper.selectEventById(eventId);

        if (event == null) {
            throw new FlowSpaceException(ErrorCode.EVENT_NOT_FOUND);
        }

        return event;
    }

    // 이벤트 시간 검증
    private void validateEventTime(LocalDateTime start, LocalDateTime end) {
        if (end != null && end.isBefore(start)) {
            throw new FlowSpaceException(ErrorCode.INVALID_EVENT_TIME);
        }
    }
}
