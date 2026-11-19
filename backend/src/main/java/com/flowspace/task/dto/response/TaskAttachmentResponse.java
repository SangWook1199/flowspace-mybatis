package com.flowspace.task.dto.response;

import java.time.LocalDateTime;

import com.flowspace.task.service.TaskAttachmentVO;

// @formatter:off

// 작업 관련 문서 응답 DTO (PAGE면 page* 값, FILE이면 file* 값 사용)
public record TaskAttachmentResponse(

    Long attachmentId,
    String type,
    Long pageId,
    String pageTitle,
    String pageIcon,
    Long fileId,
    String fileName,
    String fileUrl,
    Long fileSize,
    String mimeType,
    String createdByName,
    LocalDateTime createdAt

) {

    // TaskAttachmentVO 기반 응답 생성 (연결하지 않은 쪽은 LEFT JOIN 결과 null)
    public static TaskAttachmentResponse from(TaskAttachmentVO attachment) {
        return new TaskAttachmentResponse(
            attachment.getAttachmentId(),
            attachment.getPageId() != null ? "PAGE" : "FILE",
            attachment.getPageId(),
            attachment.getPageTitle(),
            attachment.getPageIcon(),
            attachment.getFileId(),
            attachment.getFileName(),
            attachment.getFileUrl(),
            attachment.getFileSize(),
            attachment.getMimeType(),
            attachment.getCreatedByName(),
            attachment.getCreatedAt()
        );
    }
}

// @formatter:on
