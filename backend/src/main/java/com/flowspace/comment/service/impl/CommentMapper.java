package com.flowspace.comment.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.comment.service.CommentTargetVO;
import com.flowspace.comment.service.CommentVO;
import com.flowspace.user.service.UserVO;

// 댓글 DB 접근. 메서드 이름이 Comment_SQL.xml의 id와 같아야 해요.
@Mapper
public interface CommentMapper {

    // 이메일로 사용자 번호와 닉네임 (알림 문구에 이름이 들어가요, 없으면 null)
    UserVO selectUserByEmail(String email);

    // ---------- 댓글 ----------

    // 댓글 한 건과 쓴 사람 (없으면 null)
    CommentVO selectCommentById(Long commentId);

    // 여러 작업의 댓글 전부 (원 댓글과 답글, 오래된 순)
    List<CommentVO> selectCommentListByTaskIds(@Param("taskIds") List<Long> taskIds);

    // 여러 블록의 댓글 전부 (원 댓글과 답글, 오래된 순)
    List<CommentVO> selectCommentListByBlockIds(@Param("blockIds") List<Long> blockIds);

    // 댓글 등록. 실행 후 DB가 만든 번호가 comment의 commentId에 들어가요.
    int insertComment(CommentVO comment);

    // 댓글 내용 수정
    int updateCommentContent(@Param("commentId") Long commentId, @Param("content") String content);

    // 댓글 삭제 (답글은 DB가 함께 지워요)
    int deleteComment(Long commentId);

    // ---------- 댓글이 달리는 곳 ----------

    // 작업 정보 (없으면 null)
    CommentTargetVO selectCommentTask(Long taskId);

    // 블록이 있는 페이지 정보 (프로시저 sp_comment_block, 없으면 null)
    CommentTargetVO selectCommentBlock(Long blockId);

    // 작업 담당자 번호 (댓글 알림을 받아요)
    List<Long> selectTaskAssigneeUserIds(Long taskId);

    // 주어진 사람 중 그 워크스페이스 멤버인 사람의 번호 (멘션 알림을 멤버에게만 보내요)
    List<Long> selectWorkspaceMemberIds(@Param("workspaceId") Long workspaceId,
        @Param("userIds") List<Long> userIds);
}
