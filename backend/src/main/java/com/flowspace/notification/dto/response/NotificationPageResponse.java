package com.flowspace.notification.dto.response;

import java.util.List;

// @formatter:off

// 알림 목록 응답 DTO (읽지 않은 알림 수 포함)
public record NotificationPageResponse(

    List<NotificationResponse> items,
    int page,
    boolean hasNext,
    long unreadCount

) {
}

// @formatter:on
