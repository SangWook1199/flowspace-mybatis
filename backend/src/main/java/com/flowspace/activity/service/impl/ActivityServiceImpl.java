package com.flowspace.activity.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.activity.dto.response.ActivityPageResponse;
import com.flowspace.activity.dto.response.ActivityResponse;
import com.flowspace.activity.service.ActivityService;
import com.flowspace.activity.service.ActivityVO;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.notification.service.NotificationService;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityServiceImpl implements ActivityService {

    private static final int PAGE_SIZE = 20;

    private static final int RECENT_SIZE = 7;

    // 활동 기록 SQL (같은 패키지라 import가 없어요)
    private final ActivityMapper activityMapper;
    private final WorkspaceService workspaceService;

    // 활동 기록 화면의 종류 필터 → 실제 활동 종류들
    private static final Map<String, List<ActivityType>> CATEGORY_TYPES = Map.of(
        "PAGE", List.of(ActivityType.PAGE_CREATED),
        "TASK", List.of(ActivityType.TASK_CREATED, ActivityType.TASK_COMPLETED),
        "COMMENT", List.of(ActivityType.COMMENT_CREATED),
        "SPRINT", List.of(ActivityType.SPRINT_CREATED, ActivityType.SPRINT_COMPLETED));

    // 활동 로그 저장
    @Transactional
    @Override
    public void log(Long workspaceId, Long userId, ActivityType type, ActivityTargetType targetType, Long targetId) {

        ActivityVO activity = new ActivityVO();
        activity.setWorkspaceId(workspaceId);
        activity.setUserId(userId);
        activity.setType(type);
        activity.setTargetType(targetType);
        activity.setTargetId(targetId);

        activityMapper.insertActivity(activity);
    }

    // 최근 7개 조회
    @Override
    public List<ActivityResponse> getRecentActivities(Long workspaceId, String email) {

        // 워크스페이스가 있는지, 보는 사람이 멤버인지는 워크스페이스 서비스의 규칙을 그대로 써요
        workspaceService.getWorkspace(workspaceId, email);

        return toResponses(activityMapper.selectActivityList(workspaceId, null, null, 0, RECENT_SIZE));
    }

    // 전체 조회
    @Override
    public ActivityPageResponse getActivities(Long workspaceId, Integer page, String category, Long userId,
        String email) {

        workspaceService.getWorkspace(workspaceId, email);

        int safePage = Math.max(page == null ? 0 : page, 0);

        // 모르는 종류가 오면 걸러내지 않고 전체를 보여줘요.
        List<ActivityType> types = category == null ? null : CATEGORY_TYPES.get(category);

        if (userId != null && activityMapper.selectUserCount(userId) == 0) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        // 프로시저에는 종류 목록을 쉼표로 이은 문자열로 넘겨요 (예: "TASK_CREATED,TASK_COMPLETED")
        String typeText = types == null ? null : String.join(",", types.stream().map(Enum::name).toList());

        List<ActivityResponse> items = toResponses(
            activityMapper.selectActivityList(workspaceId, userId, typeText, safePage * PAGE_SIZE, PAGE_SIZE));

        long total = activityMapper.selectActivityCount(workspaceId, userId, types);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);

        return new ActivityPageResponse(items, safePage, PAGE_SIZE, total, totalPages, safePage + 1 < totalPages);
    }

    // 조회 결과에 대상의 지금 이름과 이동 경로를 붙여요. 이름은 프로시저가 조인해 와서 여기서는 고르기만 해요.
    private List<ActivityResponse> toResponses(List<ActivityVO> activities) {

        return activities.stream().map(activity -> {
            String name = null;
            String link = null;

            switch (activity.getTargetType()) {
                case TASK -> {
                    if (activity.getTaskTitle() != null) {
                        name = activity.getTaskTitle();
                        link = NotificationService.taskLink(activity.getTaskSprintId(), activity.getTargetId());
                    }
                }
                case SPRINT -> {
                    if (activity.getSprintName() != null) {
                        name = activity.getSprintName();
                        link = "/sprints/" + activity.getTargetId();
                    }
                }
                case PAGE -> {
                    if (activity.getPageTitle() != null) {
                        name = activity.getPageTitle();
                        link = pageLink(activity.getTargetId(), activity.getPageDeleted());
                    }
                }
                case COMMENT -> {
                    // 댓글은 달린 작업(또는 페이지)의 이름과 경로를 보여줘요.
                    if (activity.getCommentTaskId() != null) {
                        name = activity.getCommentTaskTitle();
                        link = NotificationService.taskLink(activity.getCommentTaskSprintId(),
                            activity.getCommentTaskId());
                    } else if (activity.getCommentPageId() != null) {
                        name = activity.getCommentPageTitle();
                        link = pageLink(activity.getCommentPageId(), activity.getCommentPageDeleted());
                    }
                }
                default -> {
                }
            }

            return ActivityResponse.from(activity, name, link);
        }).toList();
    }

    // 휴지통에 있는 페이지는 이동할 곳이 없어요
    private String pageLink(Long pageId, Boolean deleted) {
        return Boolean.TRUE.equals(deleted) ? null : "/pages/" + pageId;
    }
}
