package com.flowspace.task.dto.request;

import jakarta.validation.constraints.NotNull;

// 관련 문서로 연결할 페이지
public record TaskAttachmentPageRequest(@NotNull Long pageId) {
}
