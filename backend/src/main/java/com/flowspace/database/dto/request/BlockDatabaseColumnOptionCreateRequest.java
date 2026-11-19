package com.flowspace.database.dto.request;

import com.flowspace.common.enums.TaskStatusCategory;
import com.flowspace.common.enums.WorkspaceColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// @formatter:off

// 컬럼 옵션 생성 요청 DTO
public record BlockDatabaseColumnOptionCreateRequest(

    @NotBlank(message = "옵션 값은 필수입니다.")
    @Size(max = 100, message = "옵션 값은 100자 이하입니다.")
    String value,

    // 색상 (null이면 GRAY)
    WorkspaceColor color,

    // 상태 그룹 (STATUS 컬럼 전용)
    TaskStatusCategory statusGroup

) {
}

// @formatter:on
