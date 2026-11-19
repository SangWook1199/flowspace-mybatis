package com.flowspace.activity.service;

import java.time.LocalDateTime;

import com.flowspace.common.enums.ActivityTargetType;
import com.flowspace.common.enums.ActivityType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 활동 기록 (등록, sp_activity_list 조회 공용)
// 대상 종류별 조인 컬럼은 해당 종류가 아니면 null
@Getter
@Setter
@NoArgsConstructor
public class ActivityVO {

    private Long activityId;

    private Long workspaceId;

    // 활동한 사람
    private Long userId;

    private ActivityType type;

    // 대상 종류 (TASK, SPRINT, PAGE, COMMENT …)와 대상 번호
    private ActivityTargetType targetType;

    private Long targetId;

    private LocalDateTime createdAt;

    // ↓ 조회할 때만 채워져요: 활동한 사람
    private String userName;

    private Long profileFileId;

    private String profileImageUrl;

    // ↓ 대상이 작업일 때
    private String taskTitle;

    private Long taskSprintId;

    // ↓ 대상이 스프린트일 때
    private String sprintName;

    // ↓ 대상이 페이지일 때
    private String pageTitle;

    private Boolean pageDeleted;

    // ↓ 대상이 댓글일 때: 댓글이 달린 작업 또는 페이지
    private Long commentTaskId;

    private String commentTaskTitle;

    private Long commentTaskSprintId;

    private Long commentPageId;

    private String commentPageTitle;

    private Boolean commentPageDeleted;
}
