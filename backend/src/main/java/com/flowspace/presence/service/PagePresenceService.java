package com.flowspace.presence.service;

import com.flowspace.websocket.PageViewEvent;
import com.flowspace.websocket.SocketClosedEvent;

// 페이지를 보는 사람·편집 중인 블록 서비스 (메모리 보관, 탭 단위)
public interface PagePresenceService {

    // 페이지 입장·이동·편집 블록 변경 처리
    void onPageView(PageViewEvent event);

    // 소켓 종료 처리
    void onClosed(SocketClosedEvent event);

    // 페이지 내용 변경 알림 (같은 페이지를 보는 다른 사람에게 PAGE_CONTENT)
    void notifyContentChanged(Long pageId, String authorEmail);
}
