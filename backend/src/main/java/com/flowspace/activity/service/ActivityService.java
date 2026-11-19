package com.flowspace.activity.service;

import java.util.List;

import com.flowspace.activity.dto.response.ActivityPageResponse;
import com.flowspace.activity.dto.response.ActivityResponse;
import com.flowspace.entity.User;
import com.flowspace.entity.Workspace;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;

// 활동 기록 서비스
// 다른 서비스는 엔티티 대신 번호(워크스페이스, 사용자)로 기록
public interface ActivityService {

    // 활동 기록 남기기 (다른 서비스에서 호출)
    void log(Long workspaceId, Long userId, ActivityType type, ActivityTargetType targetType, Long targetId);

    // 최근 7개 조회
    List<ActivityResponse> getRecentActivities(Long workspaceId, String email);

    // 전체 조회 (category: PAGE | TASK | COMMENT | SPRINT, userId: 한 사람의 활동만 — 둘 다 비우면 전체)
    ActivityPageResponse getActivities(Long workspaceId, Integer page, String category, Long userId, String email);

    // ---------- JPA 기능용 임시 메서드 (해당 기능 전환 후 삭제) ----------
    default void log(Workspace workspace, User user, ActivityType type, ActivityTargetType targetType,
        Long targetId) {
        log(workspace.getWorkspaceId(), user.getUserId(), type, targetType, targetId);
    }
}
