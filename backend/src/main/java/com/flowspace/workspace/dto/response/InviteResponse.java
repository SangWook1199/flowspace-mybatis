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

    // MyBatis 조회 결과(InviteVO)로 응답을 만들어요 (조인으로 워크스페이스·초대한 사람 이름이 들어 있어요)
    public static InviteResponse from(InviteVO invite) {
        return new InviteResponse(invite.getInviteId(), invite.getWorkspaceId(), invite.getWorkspaceName(),
            invite.getWorkspaceInitials(), invite.getWorkspaceColor().name(), invite.getInviterName(),
            invite.getStatus());
    }
}