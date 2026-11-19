package com.flowspace.presence.service;

import com.flowspace.websocket.UserConnectionEvent;

// 접속 상태 서비스 (WebSocket 연결이 하나라도 있으면 온라인)
public interface PresenceService {

    // 접속 중 여부
    boolean isOnline(Long userId);

    // 연결 변화 처리 (같은 워크스페이스 멤버에게 PRESENCE 전송)
    void onConnectionChanged(UserConnectionEvent event);
}
