package com.flowspace.comment.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 댓글 한 건이에요. 등록할 때와 조회할 때(Comment_SQL.xml의 commentSelect) 같이 써요.
// 작업 댓글이면 taskId, 페이지 블록 댓글이면 blockId가 들어 있어요.
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
