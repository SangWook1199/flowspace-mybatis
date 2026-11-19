package com.flowspace.workspace.dto.response;

import com.flowspace.entity.WorkspaceInvite;
import com.flowspace.entity.enums.InviteStatus;
import com.flowspace.workspace.service.InviteVO;

// 내가 받은 초대 응답 DTO
public record InviteResponse(

    Long inviteId, Long workspaceId, String workspaceName, String workspaceInitials, String workspaceColor,
    String inviterName, InviteStatus status) {

    public static InviteResponse from(WorkspaceInvite invite) {
        return new InviteResponse(invite.getInviteId(), invite.getWorkspace().getWorkspaceId(),
            invite.getWorkspace().getName(), invite.getWorkspace().getInitials(),
            invite.getWorkspace().getColor().name(), invite.getInviter().getNickname(), invite.getStatus());
    }

    // InviteVO 기반 응답 생성
    public static InviteResponse from(InviteVO invite) {
        return new InviteResponse(invite.getInviteId(), invite.getWorkspaceId(), invite.getWorkspaceName(),
            invite.getWorkspaceInitials(), invite.getWorkspaceColor().name(), invite.getInviterName(),
            invite.getStatus());
    }
}