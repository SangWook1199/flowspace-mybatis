package com.flowspace.comment.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 댓글 (등록, commentSelect 조회 공용)
// 작업 댓글은 taskId, 블록 댓글은 blockId 사용
@Getter
@Setter
@NoArgsConstructor
public class CommentVO {

    private Long commentId;

    private Long taskId;

    private Long blockId;

    // 쓴 사람
    private Long userId;

    // 답글이면 원 댓글 번호 (원 댓글이면 null)
    private Long parentCommentId;

    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ↓ 조회할 때만 채워져요: 쓴 사람
    private String userName;

    private Long profileFileId;

    private String profileImageUrl;
}
