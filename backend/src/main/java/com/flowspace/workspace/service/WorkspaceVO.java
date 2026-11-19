package com.flowspace.workspace.service;

import java.time.LocalDateTime;

import com.flowspace.common.enums.WorkspaceColor;
import com.flowspace.common.enums.WorkspaceRole;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 워크스페이스 (workspaces 테이블 한 행, 소유자는 번호로 보관)
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceVO {

    private Long workspaceId;

    private Long ownerId;

    private String name;

    private String initials;

    private WorkspaceColor color;

    private String icon;

    private LocalDateTime createdAt;

    // 내 워크스페이스 목록(sp_my_workspace_list)에서만 채워져요: 그 워크스페이스에서 내 역할
    private WorkspaceRole role;
}
