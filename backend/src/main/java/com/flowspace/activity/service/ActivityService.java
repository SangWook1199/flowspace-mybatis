package com.flowspace.activity.service;

import java.util.List;

import com.flowspace.activity.dto.response.ActivityPageResponse;
import com.flowspace.activity.dto.response.ActivityResponse;
import com.flowspace.common.enums.ActivityTargetType;
import com.flowspace.common.enums.ActivityType;

// 활동 기록 서비스
// 다른 서비스는 워크스페이스·사용자 번호로 기록
public interface ActivityService {

    // 활동 기록 남기기 (다른 서비스에서 호출)
    void log(Long workspaceId, Long userId, ActivityType type, ActivityTargetType targetType, Long targetId);

    // 최근 7개 조회
    List<ActivityResponse> getRecentActivities(Long workspaceId, String email);

    // 전체 조회 (category: PAGE | TASK | COMMENT | SPRINT, userId: 한 사람의 활동만 — 둘 다 비우면 전체)
    ActivityPageResponse getActivities(Long workspaceId, Integer page, String category, Long userId, String email);
}
