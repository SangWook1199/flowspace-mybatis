package com.flowspace.presence.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.flowspace.page.service.PageVO;
import com.flowspace.presence.service.PagePresenceService;
import com.flowspace.websocket.NotificationWebSocketHandler;
import com.flowspace.websocket.PageViewEvent;
import com.flowspace.websocket.SocketClosedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 메시지: { type: "PAGE_PRESENCE", data: { pageId, viewers: [{ userId, blockId }] } }
@Slf4j
@Service
@RequiredArgsConstructor
public class PagePresenceServiceImpl implements PagePresenceService {

    // 보는 사람 목록 소켓 메시지 종류
    private static final String SOCKET_TYPE_PAGE_PRESENCE = "PAGE_PRESENCE";

    // 내용 변경 소켓 메시지 종류
    private static final String SOCKET_TYPE_PAGE_CONTENT = "PAGE_CONTENT";

    // 접속 상태 SQL
    private final PresenceMapper presenceMapper;
    private final NotificationWebSocketHandler socketHandler;

    // 탭(세션) 하나가 보고 있는 페이지·블록
    private record View(Long userId, Long pageId, Long blockId) {}

    // 세션 ID별 보고 있는 곳
    private final Map<String, View> views = new HashMap<>();

    // 페이지 입장·이동·편집 블록 변경 처리 (새 페이지 입장 시에만 열람 권한 확인)
    @EventListener
    @Override
    public synchronized void onPageView(PageViewEvent event) {

        View before = views.get(event.sessionId());
        Long beforePageId = before == null ? null : before.pageId();

        if (event.pageId() == null) {
            views.remove(event.sessionId());
            broadcast(beforePageId);
            return;
        }

        if (!event.pageId().equals(beforePageId) && !canView(event.userId(), event.pageId())) {
            return;
        }

        views.put(event.sessionId(), new View(event.userId(), event.pageId(), event.blockId()));

        broadcast(event.pageId());

        if (beforePageId != null && !beforePageId.equals(event.pageId())) {
            broadcast(beforePageId);
        }
    }

    // 소켓 종료 처리 (보던 페이지에 목록 다시 전송)
    @EventListener
    @Override
    public synchronized void onClosed(SocketClosedEvent event) {

        View removed = views.remove(event.sessionId());

        if (removed != null) {
            broadcast(removed.pageId());
        }
    }

    // 페이지 내용 변경 알림 (작성자 제외, 받은 쪽이 페이지를 다시 조회)
    @Override
    public synchronized void notifyContentChanged(Long pageId, String authorEmail) {

        try {
            Long authorId = presenceMapper.selectUserIdByEmail(authorEmail);

            Set<Long> targets = new HashSet<>();

            for (View view : views.values()) {
                if (pageId.equals(view.pageId()) && !view.userId().equals(authorId)) {
                    targets.add(view.userId());
                }
            }

            if (targets.isEmpty()) {
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("pageId", pageId);
            data.put("userId", authorId);

            for (Long userId : targets) {
                socketHandler.sendToUser(userId, SOCKET_TYPE_PAGE_CONTENT, data);
            }
        } catch (Exception e) {
            log.warn("페이지 변경을 알리지 못했어요. pageId={}", pageId, e);
        }
    }

    // 페이지 열람 가능 여부 (휴지통 아님 + 워크스페이스 멤버)
    private boolean canView(Long userId, Long pageId) {

        try {
            PageVO page = presenceMapper.selectPage(pageId);

            if (page == null || Boolean.TRUE.equals(page.getIsDeleted())) {
                return false;
            }

            return presenceMapper.selectMemberCount(page.getWorkspaceId(), userId) > 0;
        } catch (Exception e) {
            log.debug("페이지 접근 확인에 실패했어요. pageId={}, userId={}", pageId, userId, e);
            return false;
        }
    }

    // 페이지를 보는 모든 사람에게 보는 사람 목록 전송 (사람당 한 줄, 탭이 여러 개면 편집 블록이 있는 쪽 우선)
    private void broadcast(Long pageId) {

        if (pageId == null) {
            return;
        }

        Map<Long, Long> byUser = new LinkedHashMap<>();

        for (View view : views.values()) {
            if (pageId.equals(view.pageId())) {
                byUser.merge(view.userId(), view.blockId() == null ? -1L : view.blockId(),
                    (a, b) -> b != -1L ? b : a);
            }
        }

        if (byUser.isEmpty()) {
            return;
        }

        List<Map<String, Object>> viewers = new ArrayList<>();

        for (Map.Entry<Long, Long> entry : byUser.entrySet()) {
            Map<String, Object> viewer = new HashMap<>();
            viewer.put("userId", entry.getKey());
            viewer.put("blockId", entry.getValue() == -1L ? null : entry.getValue());
            viewers.add(viewer);
        }

        Map<String, Object> data = Map.of("pageId", pageId, "viewers", viewers);
        Set<Long> targets = new HashSet<>(byUser.keySet());

        for (Long userId : targets) {
            socketHandler.sendToUser(userId, SOCKET_TYPE_PAGE_PRESENCE, data);
        }
    }
}
