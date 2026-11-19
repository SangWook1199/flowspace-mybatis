package com.flowspace.database.service;

import java.time.LocalDateTime;
import com.flowspace.common.enums.TaskStatusCategory;
import com.flowspace.common.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 컬럼 옵션 (SELECT/MULTI_SELECT/STATUS 선택지)
@Getter
@Setter
@NoArgsConstructor
public class BlockDatabaseColumnOptionVO {

    private Long optionId;

    private Long columnId;

    private String value;

    private WorkspaceColor color;

    // 상태 그룹 (STATUS 옵션 전용)
    private TaskStatusCategory statusGroup;

    private Integer position;

    private LocalDateTime createdAt;
}
