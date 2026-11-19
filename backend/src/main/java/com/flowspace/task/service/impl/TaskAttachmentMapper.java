package com.flowspace.task.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.task.service.TaskAttachmentVO;

// 작업 관련 문서 Mapper (TaskAttachment_SQL.xml)
@Mapper
public interface TaskAttachmentMapper {

    // 이메일로 사용자 번호 조회 (없으면 null)
    Long selectUserIdByEmail(String email);

    // 관련 문서 목록 (프로시저 sp_task_attachment_list)
    List<TaskAttachmentVO> selectTaskAttachmentList(Long taskId);

    // 관련 문서 한 건 (프로시저 sp_task_attachment, 없으면 null)
    TaskAttachmentVO selectTaskAttachmentById(Long attachmentId);

    // 작업의 관련 문서 수 (최대 수 확인용)
    int selectTaskAttachmentCount(Long taskId);

    // 이 페이지가 이미 연결돼 있는지 (0이면 없음)
    int selectTaskAttachmentPageCount(@Param("taskId") Long taskId, @Param("pageId") Long pageId);

    // 작업에 업로드한 파일 번호 (작업 삭제 시 서버 파일 정리용)
    List<Long> selectTaskAttachmentFileIds(Long taskId);

    // 연결할 페이지의 워크스페이스 번호 (없거나 휴지통에 있으면 null)
    Long selectActivePageWorkspaceId(Long pageId);

    // 관련 문서 등록 (생성된 번호는 attachmentId에 설정)
    int insertTaskAttachment(TaskAttachmentVO attachment);

    // 관련 문서 삭제
    int deleteTaskAttachment(Long attachmentId);
}
