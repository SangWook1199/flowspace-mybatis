package com.flowspace.activity.dto.response;

import java.time.LocalDateTime;

import com.flowspace.common.enums.ActivityTargetType;
import com.flowspace.common.enums.ActivityType;
import com.flowspace.activity.service.ActivityVO;

// @formatter:off

// 활동 기록 응답 DTO
// targetName: 대상의 현재 이름(작업 제목·스프린트 이름·페이지 제목, 댓글이면 댓글이 달린 작업/페이지 이름 — 지워졌으면 null)
// targetLink: 눌렀을 때 이동할 화면 경로 (이동할 곳이 없으면 null)
public record ActivityResponse(

    Long activityId,

    Long userId,
    String userName,
    Long profileFileId,
    String profileImageUrl,

    ActivityType type,
    ActivityTargetType targetType,
    Long targetId,
    String targetName,
    String targetLink,

    LocalDateTime createdAt

) {

    // ActivityVO 기반 응답 생성 (대상 이름·경로는 서비스에서 전달)
    public static ActivityResponse from(ActivityVO activity, String targetName, String targetLink) {
        return new ActivityResponse(
            activity.getActivityId(),
            activity.getUserId(),
            activity.getUserName(),
            activity.getProfileFileId(),
            activity.getProfileImageUrl(),
            activity.getType(),
            activity.getTargetType(),
            activity.getTargetId(),
            targetName,
            targetLink,
            activity.getCreatedAt()
        );
    }
}

// @formatter:on
