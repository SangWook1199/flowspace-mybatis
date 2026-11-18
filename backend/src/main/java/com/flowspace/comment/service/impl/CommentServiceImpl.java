package com.flowspace.comment.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.activity.service.ActivityService;
import com.flowspace.comment.dto.request.CommentCreateRequest;
import com.flowspace.comment.dto.request.CommentUpdateRequest;
import com.flowspace.comment.dto.response.CommentResponse;
import com.flowspace.comment.service.CommentService;
import com.flowspace.comment.service.CommentTargetVO;
import com.flowspace.comment.service.CommentVO;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.entity.enums.NotificationType;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.notification.service.NotificationService;
import com.flowspace.service.MentionParser;
import com.flowspace.user.service.UserVO;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    // 댓글 SQL (같은 패키지라 import가 없어요)
    private final CommentMapper commentMapper;
    private final WorkspaceService workspaceService;

    private final ActivityService activityService;
    private final NotificationService notificationService;

    // Task 댓글 작성
    @Override
    public CommentResponse createTaskComment(Long taskId, CommentCreateRequest request, String email) {

        UserVO actor = findActor(email);
        CommentTargetVO task = findTask(taskId);

        // 워크스페이스 멤버인지는 워크스페이스 서비스의 규칙을 그대로 써요
        workspaceService.getWorkspace(task.getWorkspaceId(), email);

        CommentVO parent = resolveParent(request.parentCommentId(), taskId, null);

        CommentVO comment = new CommentVO();
        comment.setTaskId(taskId);
        comment.setParentCommentId(parent == null ? null : parent.getCommentId());
        comment.setUserId(actor.getUserId());
        comment.setContent(request.content());

        commentMapper.insertComment(comment);

        activityService.log(task.getWorkspaceId(), actor.getUserId(), ActivityType.COMMENT_CREATED,
            ActivityTargetType.COMMENT, comment.getCommentId());

        String link = NotificationService.taskLink(task.getSprintId(), taskId);

        Set<Long> mentioned = notifyMentions(request.content(), task.getWorkspaceId(), Set.of(), actor, taskId, link);

        notifyTaskComment(task, request.content(), parent, actor, mentioned, link);

        // 쓴 사람 이름과 만든 시각까지 담으려고 다시 읽어요
        return CommentResponse.from(findComment(comment.getCommentId()), List.of());
    }

    // Block 댓글 작성
    @Override
    public CommentResponse createBlockComment(Long blockId, CommentCreateRequest request, String email) {

        UserVO actor = findActor(email);
        CommentTargetVO page = findBlockPage(blockId);

        workspaceService.getWorkspace(page.getWorkspaceId(), email);
        requireActivePage(page);

        CommentVO parent = resolveParent(request.parentCommentId(), null, blockId);

        CommentVO comment = new CommentVO();
        comment.setBlockId(blockId);
        comment.setParentCommentId(parent == null ? null : parent.getCommentId());
        comment.setUserId(actor.getUserId());
        comment.setContent(request.content());

        commentMapper.insertComment(comment);

        activityService.log(page.getWorkspaceId(), actor.getUserId(), ActivityType.COMMENT_CREATED,
            ActivityTargetType.COMMENT, comment.getCommentId());

        String link = "/pages/" + page.getTargetId();

        Set<Long> mentioned = notifyMentions(request.content(), page.getWorkspaceId(), Set.of(), actor,
            page.getTargetId(), link);

        notifyBlockComment(page, request.content(), parent, actor, mentioned, link);

        return CommentResponse.from(findComment(comment.getCommentId()), List.of());
    }

    // Task 댓글 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<CommentResponse> getTaskComments(Long taskId, String email) {

        findActor(email);
        CommentTargetVO task = findTask(taskId);
        workspaceService.getWorkspace(task.getWorkspaceId(), email);

        return getTaskCommentMap(List.of(taskId)).getOrDefault(taskId, List.of());
    }

    // Block 댓글 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<CommentResponse> getBlockComments(Long blockId, String email) {

        findActor(email);
        CommentTargetVO page = findBlockPage(blockId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        return getBlockCommentMap(List.of(blockId)).getOrDefault(blockId, List.of());
    }

    // 댓글 수정
    @Override
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, String email) {

        UserVO actor = findActor(email);
        CommentVO comment = findComment(commentId);

        if (!comment.getUserId().equals(actor.getUserId())) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }

        // 수정하면서 새로 멘션된 사람에게만 알려요(원래 멘션돼 있던 사람에게 또 보내지 않아요).
        Set<Long> alreadyMentioned = MentionParser.extractUserIds(comment.getContent());

        if (comment.getTaskId() != null) {
            CommentTargetVO task = findTask(comment.getTaskId());

            commentMapper.updateCommentContent(commentId, request.content());

            notifyMentions(request.content(), task.getWorkspaceId(), alreadyMentioned, actor, task.getTargetId(),
                NotificationService.taskLink(task.getSprintId(), task.getTargetId()));
        } else {
            CommentTargetVO page = findBlockPage(comment.getBlockId());

            // 휴지통에 있는 페이지의 블록 댓글은 고칠 수 없어요.
            requireActivePage(page);

            commentMapper.updateCommentContent(commentId, request.content());

            notifyMentions(request.content(), page.getWorkspaceId(), alreadyMentioned, actor, page.getTargetId(),
                "/pages/" + page.getTargetId());
        }

        // 바뀐 내용과 수정 시각을 담으려고 다시 읽어요
        return CommentResponse.from(findComment(commentId), List.of());
    }

    // 댓글 삭제
    @Override
    public void deleteComment(Long commentId, String email) {

        UserVO actor = findActor(email);
        CommentVO comment = findComment(commentId);

        if (!comment.getUserId().equals(actor.getUserId())) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }

        commentMapper.deleteComment(commentId);
    }

    // 여러 작업의 댓글을 한 번에 읽어서 작업 번호별로 나눠 담아요 (작업 상세, 작업 목록에서 써요)
    @Transactional(readOnly = true)
    @Override
    public Map<Long, List<CommentResponse>> getTaskCommentMap(List<Long> taskIds) {

        // 목록이 비면 IN ()이 되어 SQL 오류가 나서 여기서 끝내요
        if (taskIds.isEmpty()) {
            return Map.of();
        }

        return groupByTarget(commentMapper.selectCommentListByTaskIds(taskIds), CommentVO::getTaskId);
    }

    // 여러 블록의 댓글을 한 번에 읽어서 블록 번호별로 나눠 담아요 (페이지 상세에서 써요)
    @Transactional(readOnly = true)
    @Override
    public Map<Long, List<CommentResponse>> getBlockCommentMap(List<Long> blockIds) {

        if (blockIds.isEmpty()) {
            return Map.of();
        }

        return groupByTarget(commentMapper.selectCommentListByBlockIds(blockIds), CommentVO::getBlockId);
    }

    // 원 댓글과 답글이 섞인 목록을 "대상 번호 → 원 댓글(안에 답글)" 모양으로 바꿔요.
    // SQL이 오래된 순으로 주니까 담는 순서도 오래된 순이에요.
    private Map<Long, List<CommentResponse>> groupByTarget(List<CommentVO> comments,
        Function<CommentVO, Long> targetId) {

        // 1) 답글을 원 댓글 번호별로 모아요
        Map<Long, List<CommentResponse>> repliesByParent = new HashMap<>();

        for (CommentVO comment : comments) {
            if (comment.getParentCommentId() != null) {
                repliesByParent.computeIfAbsent(comment.getParentCommentId(), key -> new ArrayList<>())
                    .add(CommentResponse.from(comment, List.of()));
            }
        }

        // 2) 원 댓글에 답글을 붙여서 대상 번호별로 모아요
        Map<Long, List<CommentResponse>> result = new HashMap<>();

        for (CommentVO comment : comments) {
            if (comment.getParentCommentId() == null) {
                result.computeIfAbsent(targetId.apply(comment), key -> new ArrayList<>()).add(
                    CommentResponse.from(comment, repliesByParent.getOrDefault(comment.getCommentId(), List.of())));
            }
        }

        return result;
    }

    // 작업 댓글 알림: 답글이면 원 댓글 작성자에게, 그 외엔 담당자 + 작업 만든 사람에게
    // 이미 멘션 알림을 받은 사람(mentioned)에게는 같은 댓글의 다른 알림을 또 보내지 않아요.
    private void notifyTaskComment(CommentTargetVO task, String content, CommentVO parent, UserVO actor,
        Set<Long> mentioned, String link) {

        String preview = preview(content);

        if (parent != null && !mentioned.contains(parent.getUserId())) {
            notificationService.send(parent.getUserId(), actor.getUserId(), task.getWorkspaceId(),
                NotificationType.COMMENT_CREATED, actor.getNickname() + "님이 내 댓글에 답글을 남겼어요: " + preview,
                task.getTargetId(), link);
        }

        List<Long> recipients = new ArrayList<>(commentMapper.selectTaskAssigneeUserIds(task.getTargetId()));
        recipients.add(task.getCreatedBy());

        // 답글 알림을 받은 사람, 멘션 알림을 받은 사람은 빼요 (같은 댓글로 알림이 두 번 가지 않게)
        recipients.removeIf(id -> id == null || mentioned.contains(id)
            || (parent != null && id.equals(parent.getUserId())));

        notificationService.sendAll(recipients, actor.getUserId(), task.getWorkspaceId(),
            NotificationType.COMMENT_CREATED,
            actor.getNickname() + "님이 '" + task.getTitle() + "' 작업에 댓글을 남겼어요: " + preview, task.getTargetId(),
            link);
    }

    // 페이지 블록 댓글 알림: 답글이면 원 댓글 작성자에게, 그 외엔 페이지 만든 사람에게
    private void notifyBlockComment(CommentTargetVO page, String content, CommentVO parent, UserVO actor,
        Set<Long> mentioned, String link) {

        String preview = preview(content);

        if (parent != null && !mentioned.contains(parent.getUserId())) {
            notificationService.send(parent.getUserId(), actor.getUserId(), page.getWorkspaceId(),
                NotificationType.COMMENT_CREATED, actor.getNickname() + "님이 내 댓글에 답글을 남겼어요: " + preview,
                page.getTargetId(), link);
        }

        Long creatorId = page.getCreatedBy();

        boolean creatorAlreadyNotified = mentioned.contains(creatorId)
            || (parent != null && parent.getUserId().equals(creatorId));

        if (!creatorAlreadyNotified) {
            notificationService.send(creatorId, actor.getUserId(), page.getWorkspaceId(),
                NotificationType.COMMENT_CREATED,
                actor.getNickname() + "님이 '" + page.getTitle() + "' 페이지에 댓글을 남겼어요: " + preview,
                page.getTargetId(), link);
        }
    }

    // 댓글에서 멘션된 워크스페이스 멤버에게 알림을 보내고, 멘션된 멤버의 번호를 돌려줘요.
    // skip: 이미 멘션돼 있던 사람(수정할 때) — 알리지 않아요. 없는 사용자나 멤버가 아닌 번호는 무시해요.
    private Set<Long> notifyMentions(String content, Long workspaceId, Set<Long> skip, UserVO actor, Long refId,
        String link) {

        Set<Long> ids = MentionParser.extractUserIds(content);

        if (ids.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> mentionedMembers = new HashSet<>(
            commentMapper.selectWorkspaceMemberIds(workspaceId, new ArrayList<>(ids)));

        String preview = preview(content);

        for (Long targetId : mentionedMembers) {
            if (!skip.contains(targetId)) {
                notificationService.send(targetId, actor.getUserId(), workspaceId, NotificationType.COMMENT_MENTION,
                    actor.getNickname() + "님이 댓글에서 회원님을 언급했어요: " + preview, refId, link);
            }
        }

        return mentionedMembers;
    }

    // 알림에 보여줄 댓글 미리보기
    private String preview(String content) {

        if (content == null) {
            return "";
        }

        // 멘션은 "@[이름](id)" 대신 "@이름"으로 풀어서 보여줘요.
        String oneLine = MentionParser.toPlain(content).replaceAll("\\s+", " ").trim();

        return oneLine.length() > 40 ? oneLine.substring(0, 40) + "…" : oneLine;
    }

    // 답글의 부모 댓글을 확인해요.
    //  - 같은 작업(또는 같은 블록)의 댓글이어야 해요(다른 곳의 댓글을 부모로 지정하면 그쪽에 답글이 붙고 알림이 가요).
    //  - 답글에 단 답글은 그 위의 댓글에 이어 붙여요(댓글은 한 단계 답글까지만 보여줘서, 안 그러면 화면에서 사라져요).
    private CommentVO resolveParent(Long parentCommentId, Long taskId, Long blockId) {

        if (parentCommentId == null) {
            return null;
        }

        CommentVO parent = findComment(parentCommentId);

        boolean sameTarget = taskId != null ? taskId.equals(parent.getTaskId()) : blockId.equals(parent.getBlockId());

        if (!sameTarget) {
            throw new FlowSpaceException(ErrorCode.COMMENT_NOT_FOUND);
        }

        return parent.getParentCommentId() != null ? findComment(parent.getParentCommentId()) : parent;
    }

    // ---------- 조회 + 없으면 예외 ----------

    private UserVO findActor(String email) {

        UserVO user = commentMapper.selectUserByEmail(email);

        if (user == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }

    private CommentVO findComment(Long commentId) {

        CommentVO comment = commentMapper.selectCommentById(commentId);

        if (comment == null) {
            throw new FlowSpaceException(ErrorCode.COMMENT_NOT_FOUND);
        }

        return comment;
    }

    private CommentTargetVO findTask(Long taskId) {

        CommentTargetVO task = commentMapper.selectCommentTask(taskId);

        if (task == null) {
            throw new FlowSpaceException(ErrorCode.TASK_NOT_FOUND);
        }

        return task;
    }

    private CommentTargetVO findBlockPage(Long blockId) {

        CommentTargetVO page = commentMapper.selectCommentBlock(blockId);

        if (page == null) {
            throw new FlowSpaceException(ErrorCode.BLOCK_NOT_FOUND);
        }

        return page;
    }

    // 휴지통에 있는 페이지면 "페이지를 찾을 수 없음"으로 거절해요 (PageGuard와 같은 규칙)
    private void requireActivePage(CommentTargetVO page) {

        if (Boolean.TRUE.equals(page.getPageDeleted())) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }
    }
}
