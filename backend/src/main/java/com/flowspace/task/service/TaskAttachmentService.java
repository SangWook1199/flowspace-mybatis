package com.flowspace.task.service;

import java.util.List;

import com.flowspace.task.dto.request.TaskAttachmentPageRequest;
import com.flowspace.task.dto.response.TaskAttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

// TaskAttachmentServiceImpl가 실제 동작을 맡아요. Controller와 다른 서비스는 이 인터페이스만 알아요.
public interface TaskAttachmentService {

    // 관련 문서 목록 (휴지통에 있는 페이지는 빼요)
    List<TaskAttachmentResponse> getAttachments(Long taskId, String email);

    // 페이지 연결
    TaskAttachmentResponse addPage(Long taskId, TaskAttachmentPageRequest request, String email);

    // 문서 파일 올리기
    TaskAttachmentResponse addFile(Long taskId, MultipartFile multipartFile, String email);

    // 관련 문서 삭제 (올린 파일이면 서버의 파일도 같이 지워요)
    void deleteAttachment(Long taskId, Long attachmentId, String email);
}
