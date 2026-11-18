package com.flowspace.comment.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 댓글이 달리는 곳(작업 또는 페이지)의 정보예요. 권한 확인과 알림에 써요.
//  - 작업: targetId = 작업 번호 (selectCommentTask)
//  - 블록: targetId = 블록이 있는 페이지 번호 (sp_comment_block)
@Getter
@Setter
@NoArgsConstructor
public class CommentTargetVO {

    private Long targetId;

    private Long workspaceId;

    // 알림 문구에 넣는 작업·페이지 제목
    private String title;

    // 작업·페이지를 만든 사람 (알림 받는 사람)
    private Long createdBy;

    // ↓ 작업일 때만: 알림 링크에 쓰는 스프린트 (백로그면 null)
    private Long sprintId;

    // ↓ 페이지일 때만: 휴지통에 있는지
    private Boolean pageDeleted;
}
