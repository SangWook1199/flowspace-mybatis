package com.flowspace.task.dto.response;

import java.time.LocalDateTime;

import com.flowspace.task.service.TaskAttachmentVO;

// @formatter:off

// 작업 관련 문서 응답 DTO — type이 PAGE면 page* 값이, FILE이면 file* 값이 채워져요.
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

    // 프로시저 조회 결과로 만들어요. 페이지·파일 중 연결하지 않은 쪽은 LEFT JOIN이라 이미 null이에요.
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
