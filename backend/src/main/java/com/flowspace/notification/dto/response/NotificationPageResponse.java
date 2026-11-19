package com.flowspace.notification.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.flowspace.entity.Notification;

// @formatter:off

// 알림 목록 응답 DTO (읽지 않은 알림 수 포함)
public record NotificationPageResponse(

    List<NotificationResponse> items,
    int page,
    boolean hasNext,
    long unreadCount

) {

    public static NotificationPageResponse from(Page<Notification> result, long unreadCount) {
        return new NotificationPageResponse(
            result.getContent().stream().map(NotificationResponse::from).toList(),
            result.getNumber(),
            result.hasNext(),
            unreadCount
        );
    }

}

// @formatter:on
