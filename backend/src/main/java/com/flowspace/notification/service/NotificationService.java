package com.flowspace.notification.service;

import java.util.Collection;

import com.flowspace.common.enums.NotificationType;
import com.flowspace.notification.dto.response.NotificationPageResponse;
import com.flowspace.notification.dto.response.UnreadCountResponse;

// 알림 서비스
// 다른 서비스는 받는 사람·보낸 사람·워크스페이스 번호로 알림 전송
public interface NotificationService {

    // ---------- 알림 만들기 (다른 서비스에서 호출) ----------

    // 알림 저장 후 실시간 전달 (받는 사람이 없거나 본인이면 생략)
    // 호출한 쪽 트랜잭션에 포함 (롤백 시 저장되지 않음)
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

    // ---------- 작업 화면 링크 ----------

    // 작업 알림 링크: 작업이 속한 스프린트(없으면 백로그)의 작업 화면, ?task=로 상세 열기
    static String taskLink(Long sprintId, Long taskId) {
        String owner = sprintId == null ? "backlog" : String.valueOf(sprintId);
        return "/sprints/" + owner + "/tasks?task=" + taskId;
    }
}
