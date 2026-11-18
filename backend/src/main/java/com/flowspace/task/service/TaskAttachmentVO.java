package com.flowspace.task.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 작업 관련 문서 한 건이에요. 등록할 때와 조회할 때(sp_task_attachment_list, sp_task_attachment) 같이 써요.
// 페이지를 연결했으면 pageId가, 파일을 올렸으면 fileId가 들어 있어요(둘 중 하나만).
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

    // ↓ 조회할 때만 채워져요
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
