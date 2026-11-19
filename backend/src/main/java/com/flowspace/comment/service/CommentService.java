package com.flowspace.comment.service;

import java.util.List;
import java.util.Map;

import com.flowspace.comment.dto.request.CommentCreateRequest;
import com.flowspace.comment.dto.request.CommentUpdateRequest;
import com.flowspace.comment.dto.response.CommentResponse;

// 댓글 서비스
public interface CommentService {

    // Task 댓글 작성
    CommentResponse createTaskComment(Long taskId, CommentCreateRequest request, String email);

    // Block 댓글 작성
    CommentResponse createBlockComment(Long blockId, CommentCreateRequest request, String email);

    // Task 댓글 목록 조회
    List<CommentResponse> getTaskComments(Long taskId, String email);

    // Block 댓글 목록 조회
    List<CommentResponse> getBlockComments(Long blockId, String email);

    // 댓글 수정
    CommentResponse updateComment(Long commentId, CommentUpdateRequest request, String email);

    // 댓글 삭제
    void deleteComment(Long commentId, String email);

    // ---------- 상세 화면용 댓글 조회 (권한 확인은 호출하는 쪽에서) ----------

    // 작업 번호 → 그 작업의 댓글(답글 포함)
    Map<Long, List<CommentResponse>> getTaskCommentMap(List<Long> taskIds);

    // 블록 번호 → 그 블록의 댓글(답글 포함)
    Map<Long, List<CommentResponse>> getBlockCommentMap(List<Long> blockIds);
}
