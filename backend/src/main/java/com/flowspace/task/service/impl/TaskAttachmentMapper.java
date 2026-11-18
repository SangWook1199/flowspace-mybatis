package com.flowspace.task.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.task.service.TaskAttachmentVO;

// 작업 관련 문서 DB 접근. 메서드 이름이 TaskAttachment_SQL.xml의 id와 같아야 해요.
@Mapper
public interface TaskAttachmentMapper {

    // 이메일로 사용자 번호 (붙인 사람을 남길 때 써요, 없으면 null)
    Long selectUserIdByEmail(String email);

    // 관련 문서 목록 (프로시저 sp_task_attachment_list)
    List<TaskAttachmentVO> selectTaskAttachmentList(Long taskId);

    // 관련 문서 한 건 (프로시저 sp_task_attachment, 없으면 null)
    TaskAttachmentVO selectTaskAttachmentById(Long attachmentId);

    // 작업에 붙은 관련 문서 수 (한 작업에 붙일 수 있는 수를 넘지 않게)
    int selectTaskAttachmentCount(Long taskId);

    // 이 페이지가 이미 연결돼 있는지 (0이면 없음)
    int selectTaskAttachmentPageCount(@Param("taskId") Long taskId, @Param("pageId") Long pageId);

    // 작업에 올린 파일 번호들 (작업을 지울 때 서버 파일도 지우려고 읽어요)
    List<Long> selectTaskAttachmentFileIds(Long taskId);

    // 연결할 페이지의 워크스페이스 번호 (없거나 휴지통에 있으면 null)
    Long selectActivePageWorkspaceId(Long pageId);

    // 관련 문서 등록. 실행 후 DB가 만든 번호가 attachment의 attachmentId에 들어가요.
    int insertTaskAttachment(TaskAttachmentVO attachment);

    // 관련 문서 삭제
    int deleteTaskAttachment(Long attachmentId);
}
