package com.flowspace.comment.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.flowspace.comment.service.CommentVO;

// @formatter:off

// 댓글 응답 DTO
public record CommentResponse(

    Long commentId,
    Long parentCommentId,
    Long userId,
    String userName,
    Long profileFileId,
    String profileImageUrl,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<CommentResponse> replies

) {

    // CommentVO 기반 응답 생성
    public static CommentResponse from(
        CommentVO comment,
        List<CommentResponse> replies
    ) {
        return new CommentResponse(
            comment.getCommentId(),
            comment.getParentCommentId(),
            comment.getUserId(),
            comment.getUserName(),
            comment.getProfileFileId(),
            comment.getProfileImageUrl(),
            comment.getContent(),
            comment.getCreatedAt(),
            comment.getUpdatedAt(),
            replies
        );
    }

}

// @formatter:on