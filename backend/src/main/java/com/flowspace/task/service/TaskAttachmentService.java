package com.flowspace.task.service;

import java.util.List;

import com.flowspace.task.dto.request.TaskAttachmentPageRequest;
import com.flowspace.task.dto.response.TaskAttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

// 작업 관련 문서 서비스
public interface TaskAttachmentService {

    // 관련 문서 목록 (휴지통 페이지 제외)
    List<TaskAttachmentResponse> getAttachments(Long taskId, String email);

    // 페이지 연결
    TaskAttachmentResponse addPage(Long taskId, TaskAttachmentPageRequest request, String email);

    // 문서 파일 올리기
    TaskAttachmentResponse addFile(Long taskId, MultipartFile multipartFile, String email);

    // 관련 문서 삭제 (업로드 파일이면 서버 파일도 삭제)
    void deleteAttachment(Long taskId, Long attachmentId, String email);
}
