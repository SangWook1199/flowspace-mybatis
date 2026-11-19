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

    // 댓글 SQL
    private final CommentMapper commentMapper;
    private final WorkspaceService workspaceService;

    private final ActivityService activityService;
    private final NotificationService notificationService;

    // Task 댓글 작성
    @Override
    public CommentResponse createTaskComment(Long taskId, CommentCreateRequest request, String email) {

        UserVO actor = findActor(email);
        CommentTargetVO task = findTask(taskId);

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

        Set<Long> alreadyMentioned = MentionParser.extractUserIds(comment.getContent());

        if (comment.getTaskId() != null) {
            CommentTargetVO task = findTask(comment.getTaskId());

            commentMapper.updateCommentContent(commentId, request.content());

            notifyMentions(request.content(), task.getWorkspaceId(), alreadyMentioned, actor, task.getTargetId(),
                NotificationService.taskLink(task.getSprintId(), task.getTargetId()));
        } else {
            CommentTargetVO page = findBlockPage(comment.getBlockId());

            requireActivePage(page);

            commentMapper.updateCommentContent(commentId, request.content());

            notifyMentions(request.content(), page.getWorkspaceId(), alreadyMentioned, actor, page.getTargetId(),
                "/pages/" + page.getTargetId());
        }

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

    // 여러 작업의 댓글을 작업 번호별로 묶어 반환 (작업 상세·목록용)
    @Transactional(readOnly = true)
    @Override
    public Map<Long, List<CommentResponse>> getTaskCommentMap(List<Long> taskIds) {

        if (taskIds.isEmpty()) {
            return Map.of();
        }

        return groupByTarget(commentMapper.selectCommentListByTaskIds(taskIds), CommentVO::getTaskId);
    }

    // 여러 블록의 댓글을 블록 번호별로 묶어 반환 (페이지 상세용)
    @Transactional(readOnly = true)
    @Override
    public Map<Long, List<CommentResponse>> getBlockCommentMap(List<Long> blockIds) {

        if (blockIds.isEmpty()) {
            return Map.of();
        }

        return groupByTarget(commentMapper.selectCommentListByBlockIds(blockIds), CommentVO::getBlockId);
    }

    // 원 댓글·답글 목록을 "대상 번호 → 원 댓글(답글 포함)" 형태로 변환 (오래된 순 유지)
    private Map<Long, List<CommentResponse>> groupByTarget(List<CommentVO> comments,
        Function<CommentVO, Long> targetId) {

        Map<Long, List<CommentResponse>> repliesByParent = new HashMap<>();

        for (CommentVO comment : comments) {
            if (comment.getParentCommentId() != null) {
                repliesByParent.computeIfAbsent(comment.getParentCommentId(), key -> new ArrayList<>())
                    .add(CommentResponse.from(comment, List.of()));
            }
        }

        Map<Long, List<CommentResponse>> result = new HashMap<>();

        for (CommentVO comment : comments) {
            if (comment.getParentCommentId() == null) {
                result.computeIfAbsent(targetId.apply(comment), key -> new ArrayList<>()).add(
                    CommentResponse.from(comment, repliesByParent.getOrDefault(comment.getCommentId(), List.of())));
            }
        }

        return result;
    }

    // 작업 댓글 알림: 답글이면 원 댓글 작성자, 아니면 담당자 + 작업 작성자
    // 멘션 알림을 받은 사용자는 제외
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

        recipients.removeIf(id -> id == null || mentioned.contains(id)
            || (parent != null && id.equals(parent.getUserId())));

        notificationService.sendAll(recipients, actor.getUserId(), task.getWorkspaceId(),
            NotificationType.COMMENT_CREATED,
            actor.getNickname() + "님이 '" + task.getTitle() + "' 작업에 댓글을 남겼어요: " + preview, task.getTargetId(),
            link);
    }

    // 블록 댓글 알림: 답글이면 원 댓글 작성자, 아니면 페이지 작성자
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

    // 멘션된 워크스페이스 멤버에게 알림 후 멘션된 멤버 번호 반환
    // skip: 수정 전부터 멘션돼 있던 사용자 (알림 제외), 멤버가 아닌 번호는 무시
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

        String oneLine = MentionParser.toPlain(content).replaceAll("\\s+", " ").trim();

        return oneLine.length() > 40 ? oneLine.substring(0, 40) + "…" : oneLine;
    }

    // 답글의 부모 댓글 확인
    // - 같은 작업(또는 블록)의 댓글만 허용
    // - 답글의 답글은 상위 원 댓글에 연결 (답글은 한 단계까지만 표시)
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

    // 휴지통 페이지면 PAGE_NOT_FOUND (PageGuard와 같은 규칙)
    private void requireActivePage(CommentTargetVO page) {

        if (Boolean.TRUE.equals(page.getPageDeleted())) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }
    }
}
