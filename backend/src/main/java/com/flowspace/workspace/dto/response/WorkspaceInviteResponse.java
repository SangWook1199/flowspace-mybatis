package com.flowspace.workspace.dto.response;

import com.flowspace.entity.WorkspaceInvite;
import com.flowspace.entity.enums.InviteStatus;
import com.flowspace.workspace.service.InviteVO;

// @formatter:off

// 워크스페이스 초대 응답 DTO
public record WorkspaceInviteResponse(

        Long inviteId,
        Long workspaceId,
        Long inviterId,
        String email,
        InviteStatus status
) {

    public static WorkspaceInviteResponse from(WorkspaceInvite invite) {
        return new WorkspaceInviteResponse(
                invite.getInviteId(),
                invite.getWorkspace().getWorkspaceId(),
                invite.getInviter().getUserId(),
                invite.getEmail(),
                invite.getStatus()
        );
    }

    // InviteVO 기반 응답 생성
    public static WorkspaceInviteResponse from(InviteVO invite) {
        return new WorkspaceInviteResponse(
                invite.getInviteId(),
                invite.getWorkspaceId(),
                invite.getInviterId(),
                invite.getEmail(),
                invite.getStatus()
        );
    }
}

// @formatter:on