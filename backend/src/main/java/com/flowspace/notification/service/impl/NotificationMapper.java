package com.flowspace.notification.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.common.enums.NotificationType;
import com.flowspace.notification.service.DueSoonTargetVO;
import com.flowspace.notification.service.NotificationVO;

// 알림 Mapper (Notification_SQL.xml)
@Mapper
public interface NotificationMapper {

    // 이메일로 사용자 번호 조회
    Long selectUserIdByEmail(String email);

    // 알림 등록 (생성된 번호는 notificationId에 설정)
    int insertNotification(NotificationVO notification);

    // 알림 한 건 (프로시저 sp_notification, 없으면 null)
    NotificationVO selectNotificationById(Long notificationId);

    // 내 알림 목록 (프로시저 sp_notification_list)
    List<NotificationVO> selectNotificationList(@Param("userId") Long userId, @Param("offset") int offset,
        @Param("limit") int limit);

    // 읽지 않은 알림 수
    long selectUnreadNotificationCount(Long userId);

    // 알림을 받은 사용자 번호 (본인 알림 확인용, 없으면 null)
    Long selectNotificationUserId(Long notificationId);

    // 알림 한 건 읽음
    int updateNotificationRead(Long notificationId);

    // 내 알림 모두 읽음
    int updateAllNotificationsRead(Long userId);

    // 특정 대상에 대한 내 알림 읽음 (초대를 수락·거절하면 그 초대 알림)
    int updateNotificationReadByRef(@Param("userId") Long userId, @Param("type") NotificationType type,
        @Param("refId") Long refId);

    // 알림 한 건 삭제
    int deleteNotification(Long notificationId);

    // 보관 기간이 지난 알림 삭제 (삭제 건수 반환)
    int deleteOldNotifications(LocalDateTime before);

    // 마감 임박 알림을 보낼 대상 (프로시저 sp_due_soon_target_list)
    List<DueSoonTargetVO> selectDueSoonTargetList(@Param("endDate") LocalDate endDate,
        @Param("since") LocalDateTime since);
}
