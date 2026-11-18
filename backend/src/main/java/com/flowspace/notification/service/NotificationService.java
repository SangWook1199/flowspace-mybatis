package com.flowspace.notification.service;

import java.util.Collection;
import java.util.Objects;

import com.flowspace.entity.Task;
import com.flowspace.entity.User;
import com.flowspace.entity.Workspace;
import com.flowspace.entity.enums.NotificationType;
import com.flowspace.notification.dto.response.NotificationPageResponse;
import com.flowspace.notification.dto.response.UnreadCountResponse;

// 알림 기능의 목록이에요. 실제 동작은 service.impl의 NotificationServiceImpl에 있어요.
// 다른 서비스는 엔티티가 아니라 번호(받는 사람, 보낸 사람, 워크스페이스)로 알림을 보내요.
public interface NotificationService {

    // ---------- 알림 만들기 (다른 서비스에서 호출) ----------

    // 알림 한 건 저장 후 실시간 전달. 받는 사람이 없거나(null) 내가 일으킨 일이면 보내지 않아요.
    // 호출한 쪽의 트랜잭션에 같이 묶여서, 그쪽이 롤백되면 알림도 저장되지 않아요.
    void send(Long recipientId, Long actorId, Long workspaceId, NotificationType type, String message, Long refId,
        String linkPath);

    // 여러 명에게 같은 알림 (같은 사람은 한 번만)
    void sendAll(Collection<Long> recipientIds, Long actorId, Long workspaceId, NotificationType type, String message,
        Long refId, String linkPath);

    // 특정 대상에 대한 내 알림을 읽음 처리 (초대를 수락/거절하면 그 초대 알림을 읽음으로)
    void markReadByRef(Long userId, NotificationType type, Long refId);

    // ---------- 조회 · 읽음 처리 (API) ----------

    // 내 알림 목록 (최신순)
    NotificationPageResponse getNotifications(String email, int page, int size);

    // 읽지 않은 알림 수
    UnreadCountResponse getUnreadCount(String email);

    // 알림 한 건 읽음
    void markRead(Long notificationId, String email);

    // 모두 읽음
    void markAllRead(String email);

    // 알림 한 건 삭제
    void delete(Long notificationId, String email);

    // ---------- 정기 작업 (NotificationScheduler가 호출) ----------

    // 오늘·내일이 마감인데 아직 끝나지 않은 작업의 담당자에게 알려요(같은 날 같은 작업은 한 번만).
    int sendDueSoonNotifications();

    // 오래된 알림 정리 (보관 기간이 지난 알림 삭제)
    int deleteOldNotifications(int retentionDays);

    // ---------- 작업 화면 링크 (여러 기능이 같이 써요) ----------

    // 작업 알림을 누르면 가는 화면: 그 작업이 있는 스프린트(없으면 백로그)의 작업 화면이고, ?task=로 그 작업 상세를 열어줘요.
    static String taskLink(Long sprintId, Long taskId) {
        String owner = sprintId == null ? "backlog" : String.valueOf(sprintId);
        return "/sprints/" + owner + "/tasks?task=" + taskId;
    }

    // ---------- 아직 JPA인 기능(댓글, 회고 등)을 위한 임시 메서드 ----------
    // 엔티티에서 번호만 꺼내 위 메서드를 불러요. 그 기능들을 MyBatis로 바꾸면 지워요.

    static String taskLink(Task task) {
        return taskLink(task.getSprint() == null ? null : task.getSprint().getSprintId(), task.getTaskId());
    }

    default void send(User recipient, User actor, Workspace workspace, NotificationType type, String message,
        Long refId, String linkPath) {
        send(recipient == null ? null : recipient.getUserId(), actor == null ? null : actor.getUserId(),
            workspace == null ? null : workspace.getWorkspaceId(), type, message, refId, linkPath);
    }

    default void sendAll(Collection<User> recipients, User actor, Workspace workspace, NotificationType type,
        String message, Long refId, String linkPath) {
        sendAll(recipients.stream().filter(Objects::nonNull).map(User::getUserId).toList(),
            actor == null ? null : actor.getUserId(), workspace == null ? null : workspace.getWorkspaceId(), type,
            message, refId, linkPath);
    }
}
