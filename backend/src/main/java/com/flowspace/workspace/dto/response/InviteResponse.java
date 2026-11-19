package com.flowspace.workspace.dto.response;

import com.flowspace.common.enums.InviteStatus;
import com.flowspace.workspace.service.InviteVO;

// 내가 받은 초대 응답 DTO
public record InviteResponse(

    Long inviteId, Long workspaceId, String workspaceName, String workspaceInitials, String workspaceColor,
    String inviterName, InviteStatus status) {

    // InviteVO 기반 응답 생성
    public static InviteResponse from(InviteVO invite) {
        return new InviteResponse(invite.getInviteId(), invite.getWorkspaceId(), invite.getWorkspaceName(),
            invite.getWorkspaceInitials(), invite.getWorkspaceColor().name(), invite.getInviterName(),
            invite.getStatus());
    }
}