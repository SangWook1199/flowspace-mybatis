package com.flowspace.notification.service;

import java.time.LocalDateTime;

import com.flowspace.common.enums.NotificationType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 알림 (등록, sp_notification_list·sp_notification 조회 공용)
@Getter
@Setter
@NoArgsConstructor
public class NotificationVO {

    private Long notificationId;

    // 받는 사람
    private Long userId;

    // 일으킨 사람 (시스템 알림이면 null)
    private Long actorId;

    private Long workspaceId;

    private NotificationType type;

    private String message;

    // 알림이 가리키는 대상 번호 (작업, 초대 등)
    private Long refId;

    // 알림을 누르면 이동할 화면 경로
    private String linkPath;

    private Boolean isRead;

    private LocalDateTime createdAt;

    // ↓ 조회 전용 (조인 컬럼)
    private String workspaceName;

    private String actorName;
}
