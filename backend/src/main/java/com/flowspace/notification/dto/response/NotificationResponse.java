package com.flowspace.notification.dto.response;

import com.flowspace.common.enums.NotificationType;
import com.flowspace.notification.service.NotificationVO;

// @formatter:off

// 알림 응답 DTO (WebSocket 알림도 같은 형식, createdAt은 ISO 문자열)
public record NotificationResponse(

    Long notificationId,
    NotificationType type,
    String message,

    Long workspaceId,
    String workspaceName,

    Long actorId,
    String actorName,

    Long refId,
    String linkPath,

    boolean read,
    String createdAt

) {

    // NotificationVO 기반 응답 생성
    public static NotificationResponse from(NotificationVO notification) {
        return new NotificationResponse(
            notification.getNotificationId(),
            notification.getType(),
            notification.getMessage(),
            notification.getWorkspaceId(),
            notification.getWorkspaceName(),
            notification.getActorId(),
            notification.getActorName(),
            notification.getRefId(),
            notification.getLinkPath(),
            Boolean.TRUE.equals(notification.getIsRead()),
            notification.getCreatedAt() == null ? null : notification.getCreatedAt().toString()
        );
    }
}

// @formatter:on
