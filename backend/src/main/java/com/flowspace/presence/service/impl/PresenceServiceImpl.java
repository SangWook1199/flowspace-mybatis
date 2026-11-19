package com.flowspace.presence.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.flowspace.presence.service.PresenceService;
import com.flowspace.websocket.NotificationWebSocketHandler;
import com.flowspace.websocket.UserConnectionEvent;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 메시지: { type: "PRESENCE", data: { userId, online, lastActiveAt } }
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    // 오프라인 알림 유예 시간 (새로고침·재연결 중 깜빡임 방지)
    private static final long OFFLINE_GRACE_SECONDS = 3;

    // 접속 상태 소켓 메시지 종류
    private static final String SOCKET_TYPE_PRESENCE = "PRESENCE";

    // 접속 상태 SQL
    private final PresenceMapper presenceMapper;
    private final NotificationWebSocketHandler socketHandler;

    // 오프라인 지연 확인용 스레드
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "presence-offline");
        thread.setDaemon(true);
        return thread;
    });

    // 접속 중 여부
    @Override
    public boolean isOnline(Long userId) {
        return socketHandler.isOnline(userId);
    }

    // 연결 변화 처리 (연결 시 바로 온라인, 끊김 시 유예 후에도 연결이 없으면 오프라인)
    @EventListener
    @Override
    public void onConnectionChanged(UserConnectionEvent event) {

        if (event.online()) {
            touchLastActive(event.userId());
            broadcast(event.userId(), true, null);
            return;
        }

        scheduler.schedule(() -> {
            if (!socketHandler.isOnline(event.userId())) {
                LocalDateTime lastActiveAt = touchLastActive(event.userId());
                broadcast(event.userId(), false, lastActiveAt);
            }
        }, OFFLINE_GRACE_SECONDS, TimeUnit.SECONDS);
    }

    // 마지막 접속 시각 갱신 (실패하면 null)
    private LocalDateTime touchLastActive(Long userId) {

        LocalDateTime now = LocalDateTime.now();

        try {
            presenceMapper.updateLastActiveAt(userId, now);
            return now;
        } catch (Exception e) {
            log.warn("마지막 접속 시각을 갱신하지 못했어요. userId={}", userId, e);
            return null;
        }
    }

    // 같은 워크스페이스 멤버에게 상태 변화 전송 (오프라인이면 마지막 접속 시각 포함)
    private void broadcast(Long userId, boolean online, LocalDateTime lastActiveAt) {

        try {
            List<Long> coMemberIds = presenceMapper.selectCoMemberIds(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("online", online);
            data.put("lastActiveAt", lastActiveAt == null ? null : lastActiveAt.toString());

            for (Long coMemberId : coMemberIds) {
                socketHandler.sendToUser(coMemberId, SOCKET_TYPE_PRESENCE, data);
            }
        } catch (Exception e) {
            log.warn("접속 상태를 알리지 못했어요. userId={}", userId, e);
        }
    }

    // 종료 시 스레드 정리
    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
