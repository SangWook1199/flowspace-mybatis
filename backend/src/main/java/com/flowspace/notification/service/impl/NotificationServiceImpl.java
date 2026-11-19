package com.flowspace.notification.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.flowspace.common.enums.NotificationType;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.notification.dto.response.NotificationPageResponse;
import com.flowspace.notification.dto.response.NotificationResponse;
import com.flowspace.notification.dto.response.UnreadCountResponse;
import com.flowspace.notification.service.DueSoonTargetVO;
import com.flowspace.notification.service.NotificationService;
import com.flowspace.notification.service.NotificationVO;
import com.flowspace.websocket.NotificationWebSocketHandler;

import lombok.RequiredArgsConstructor;

// 알림 저장 · 조회 · 읽음 처리, 그리고 WebSocket으로 실시간 전달
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 50;

    // WebSocket으로 보내는 메시지의 type 값
    private static final String SOCKET_TYPE_NOTIFICATION = "NOTIFICATION";

    // 알림 SQL
    private final NotificationMapper notificationMapper;
    private final NotificationWebSocketHandler socketHandler;

    // ---------- 알림 만들기 (다른 서비스에서 호출) ----------

    @Transactional
    @Override
    public void send(Long recipientId, Long actorId, Long workspaceId, NotificationType type, String message,
        Long refId, String linkPath) {

        if (recipientId == null) {
            return;
        }

        if (Objects.equals(actorId, recipientId)) {
            return;
        }

        NotificationVO notification = new NotificationVO();
        notification.setUserId(recipientId);
        notification.setActorId(actorId);
        notification.setWorkspaceId(workspaceId);
        notification.setType(type);
        notification.setMessage(truncate(message));
        notification.setRefId(refId);
        notification.setLinkPath(linkPath);

        notificationMapper.insertNotification(notification);

        publish(recipientId, NotificationResponse.from(notificationMapper.selectNotificationById(
            notification.getNotificationId())));
    }

    @Transactional
    @Override
    public void sendAll(Collection<Long> recipientIds, Long actorId, Long workspaceId, NotificationType type,
        String message, Long refId, String linkPath) {

        Set<Long> unique = new LinkedHashSet<>();

        for (Long recipientId : recipientIds) {
            if (recipientId != null) {
                unique.add(recipientId);
            }
        }

        for (Long recipientId : unique) {
            send(recipientId, actorId, workspaceId, type, message, refId, linkPath);
        }
    }

    @Transactional
    @Override
    public void markReadByRef(Long userId, NotificationType type, Long refId) {
        notificationMapper.updateNotificationReadByRef(userId, type, refId);
    }

    // ---------- 조회 · 읽음 처리 (API) ----------

    @Override
    public NotificationPageResponse getNotifications(String email, int page, int size) {

        Long userId = findUserId(email);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        List<NotificationVO> rows = notificationMapper.selectNotificationList(userId, safePage * safeSize,
            safeSize + 1);

        boolean hasNext = rows.size() > safeSize;

        List<NotificationResponse> items = rows.stream().limit(safeSize).map(NotificationResponse::from).toList();

        return new NotificationPageResponse(items, safePage, hasNext,
            notificationMapper.selectUnreadNotificationCount(userId));
    }

    @Override
    public UnreadCountResponse getUnreadCount(String email) {
        return new UnreadCountResponse(notificationMapper.selectUnreadNotificationCount(findUserId(email)));
    }

    @Transactional
    @Override
    public void markRead(Long notificationId, String email) {
        checkOwnNotification(notificationId, email);
        notificationMapper.updateNotificationRead(notificationId);
    }

    @Transactional
    @Override
    public void markAllRead(String email) {
        notificationMapper.updateAllNotificationsRead(findUserId(email));
    }

    @Transactional
    @Override
    public void delete(Long notificationId, String email) {
        checkOwnNotification(notificationId, email);
        notificationMapper.deleteNotification(notificationId);
    }

    // ---------- 정기 작업 (NotificationScheduler가 호출) ----------

    @Transactional
    @Override
    public int sendDueSoonNotifications() {

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        int sent = 0;

        for (int plusDays = 0; plusDays <= 1; plusDays++) {

            String when = plusDays == 0 ? "오늘" : "내일";

            for (DueSoonTargetVO target : notificationMapper.selectDueSoonTargetList(today.plusDays(plusDays),
                startOfToday)) {

                send(target.getUserId(), null, target.getWorkspaceId(), NotificationType.TASK_DUE_SOON,
                    "'" + target.getTitle() + "' 작업의 마감이 " + when + "이에요.", target.getTaskId(),
                    NotificationService.taskLink(target.getSprintId(), target.getTaskId()));
                sent++;
            }
        }

        return sent;
    }

    @Transactional
    @Override
    public int deleteOldNotifications(int retentionDays) {
        return notificationMapper.deleteOldNotifications(LocalDateTime.now().minusDays(retentionDays));
    }

    // ---------- 공통 ----------

    // 커밋 후 전송 (롤백된 알림 표시 방지), 트랜잭션 밖이면 즉시 전송
    private void publish(Long userId, NotificationResponse payload) {

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.sendToUser(userId, SOCKET_TYPE_NOTIFICATION, payload);
                }
            });
        } else {
            socketHandler.sendToUser(userId, SOCKET_TYPE_NOTIFICATION, payload);
        }
    }

    private String truncate(String message) {
        return message != null && message.length() > 300 ? message.substring(0, 299) + "…" : message;
    }

    private Long findUserId(String email) {

        Long userId = notificationMapper.selectUserIdByEmail(email);

        if (userId == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        return userId;
    }

    // 본인 알림만 처리 가능
    private void checkOwnNotification(Long notificationId, String email) {

        Long ownerId = notificationMapper.selectNotificationUserId(notificationId);

        if (ownerId == null) {
            throw new FlowSpaceException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        if (!ownerId.equals(findUserId(email))) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }
    }
}
