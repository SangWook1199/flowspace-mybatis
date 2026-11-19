package com.flowspace.comment.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.comment.service.CommentTargetVO;
import com.flowspace.comment.service.CommentVO;
import com.flowspace.user.service.UserVO;

// 댓글 Mapper (Comment_SQL.xml)
@Mapper
public interface CommentMapper {

    // 이메일로 사용자 번호·닉네임 조회 (없으면 null)
    UserVO selectUserByEmail(String email);

    // ---------- 댓글 ----------

    // 댓글 한 건과 쓴 사람 (없으면 null)
    CommentVO selectCommentById(Long commentId);

    // 여러 작업의 댓글 전부 (원 댓글과 답글, 오래된 순)
    List<CommentVO> selectCommentListByTaskIds(@Param("taskIds") List<Long> taskIds);

    // 여러 블록의 댓글 전부 (원 댓글과 답글, 오래된 순)
    List<CommentVO> selectCommentListByBlockIds(@Param("blockIds") List<Long> blockIds);

    // 댓글 등록 (생성된 번호는 commentId에 설정)
    int insertComment(CommentVO comment);

    // 댓글 내용 수정
    int updateCommentContent(@Param("commentId") Long commentId, @Param("content") String content);

    // 댓글 삭제 (답글은 DB에서 연쇄 삭제)
    int deleteComment(Long commentId);

    // ---------- 댓글이 달리는 곳 ----------

    // 작업 정보 (없으면 null)
    CommentTargetVO selectCommentTask(Long taskId);

    // 블록이 있는 페이지 정보 (프로시저 sp_comment_block, 없으면 null)
    CommentTargetVO selectCommentBlock(Long blockId);

    // 작업 담당자 번호 (댓글 알림 대상)
    List<Long> selectTaskAssigneeUserIds(Long taskId);

    // 주어진 사용자 중 워크스페이스 멤버 번호 (멘션 알림 대상)
    List<Long> selectWorkspaceMemberIds(@Param("workspaceId") Long workspaceId,
        @Param("userIds") List<Long> userIds);
}
