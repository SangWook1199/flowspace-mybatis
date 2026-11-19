package com.flowspace.task.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 작업 관련 문서 (등록, sp_task_attachment_list·sp_task_attachment 조회 공용)
// 페이지 연결은 pageId, 파일 업로드는 fileId (둘 중 하나)
@Getter
@Setter
@NoArgsConstructor
public class TaskAttachmentVO {

    private Long attachmentId;

    private Long taskId;

    private Long pageId;

    private Long fileId;

    // 붙인 사람
    private Long createdBy;

    private LocalDateTime createdAt;

    // ↓ 조회 전용
    private String createdByName;

    // 페이지를 연결했을 때
    private String pageTitle;

    private String pageIcon;

    private Boolean pageDeleted;

    // 파일을 올렸을 때
    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String mimeType;
}
