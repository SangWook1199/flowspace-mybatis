package com.flowspace.calendar.web;

import com.flowspace.calendar.dto.request.EventCreateRequest;
import com.flowspace.calendar.dto.response.EventResponse;
import com.flowspace.calendar.dto.response.EventSearchResponse;
import com.flowspace.calendar.dto.request.EventUpdateRequest;
import com.flowspace.calendar.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "OAuth2")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "이벤트 생성")
    @PostMapping("/workspaces/{workspaceId}/events")
    public EventResponse createEvent(@PathVariable Long workspaceId, @Valid @RequestBody EventCreateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        return eventService.createEvent(workspaceId, request, userDetails.getUsername());
    }

    @Operation(summary = "이벤트 단건 조회")
    @GetMapping("/events/{eventId}")
    public EventResponse getEvent(@PathVariable Long eventId, @AuthenticationPrincipal UserDetails userDetails) {
        return eventService.getEvent(eventId, userDetails.getUsername());
    }

    @Operation(summary = "이벤트 수정")
    @PatchMapping("/events/{eventId}")
    public EventResponse updateEvent(@PathVariable Long eventId, @Valid @RequestBody EventUpdateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        return eventService.updateEvent(eventId, request, userDetails.getUsername());
    }

    @Operation(summary = "이벤트 삭제")
    @DeleteMapping("/events/{eventId}")
    public void deleteEvent(@PathVariable Long eventId, @AuthenticationPrincipal UserDetails userDetails) {
        eventService.deleteEvent(eventId, userDetails.getUsername());
    }

    @Operation(summary = "Event 검색")
    @GetMapping("/workspaces/{workspaceId}/events/search")
    public List<EventSearchResponse> searchEvents(@PathVariable Long workspaceId,
        @RequestParam(required = false) String keyword, @AuthenticationPrincipal UserDetails userDetails) {
        return eventService.searchEvents(workspaceId, keyword, userDetails.getUsername());
    }
}