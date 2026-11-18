package com.flowspace.activity.service;

import java.util.List;

import com.flowspace.activity.dto.response.ActivityPageResponse;
import com.flowspace.activity.dto.response.ActivityResponse;
import com.flowspace.entity.User;
import com.flowspace.entity.Workspace;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;

// 활동 기록 기능의 목록이에요. 실제 동작은 service.impl의 ActivityServiceImpl에 있어요.
// 다른 서비스는 엔티티가 아니라 번호(워크스페이스, 사람)로 기록을 남겨요.
public interface ActivityService {

    // 활동 기록 남기기 (다른 서비스에서 호출)
    void log(Long workspaceId, Long userId, ActivityType type, ActivityTargetType targetType, Long targetId);

    // 최근 7개 조회
    List<ActivityResponse> getRecentActivities(Long workspaceId, String email);

    // 전체 조회 (category: PAGE | TASK | COMMENT | SPRINT, userId: 한 사람의 활동만 — 둘 다 비우면 전체)
    ActivityPageResponse getActivities(Long workspaceId, Integer page, String category, Long userId, String email);

    // ---------- 아직 JPA인 기능(댓글, 페이지, AI, 회고)을 위한 임시 메서드 ----------
    // 엔티티에서 번호만 꺼내 위 메서드를 불러요. 그 기능들을 MyBatis로 바꾸면 지워요.
    default void log(Workspace workspace, User user, ActivityType type, ActivityTargetType targetType,
        Long targetId) {
        log(workspace.getWorkspaceId(), user.getUserId(), type, targetType, targetId);
    }
}
