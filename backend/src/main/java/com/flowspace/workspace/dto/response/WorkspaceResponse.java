package com.flowspace.workspace.dto.response;

import com.flowspace.entity.Workspace;
import com.flowspace.entity.enums.WorkspaceColor;
import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.workspace.service.WorkspaceVO;

// @formatter:off

// 워크스페이스 응답 DTO
public record WorkspaceResponse(

        Long workspaceId,
        String name,
        String initials,
        WorkspaceColor color,
        String icon,
        Long ownerId,
        WorkspaceRole role
) {

    public static WorkspaceResponse from(
            Workspace workspace,
            WorkspaceRole role
    ) {
        return new WorkspaceResponse(
                workspace.getWorkspaceId(),
                workspace.getName(),
                workspace.getInitials(),
                workspace.getColor(),
                workspace.getIcon(),
                workspace.getOwner().getUserId(),
                role
        );
    }

    // WorkspaceVO 기반 응답 생성
    public static WorkspaceResponse from(
            WorkspaceVO workspace,
            WorkspaceRole role
    ) {
        return new WorkspaceResponse(
                workspace.getWorkspaceId(),
                workspace.getName(),
                workspace.getInitials(),
                workspace.getColor(),
                workspace.getIcon(),
                workspace.getOwnerId(),
                role
        );
    }
}

// @formatter:on