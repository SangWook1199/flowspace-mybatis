package com.flowspace.workspace.service;

import java.time.LocalDateTime;

import com.flowspace.entity.enums.InviteStatus;
import com.flowspace.entity.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 워크스페이스 초대 한 건을 담는 객체예요. 초대를 등록할 때와 조회할 때 같이 써요.
@Getter
@Setter
@NoArgsConstructor
public class InviteVO {

    private Long inviteId;

    private Long workspaceId;

    // 초대한 사람 번호
    private Long inviterId;

    // 초대받은 사람 이메일
    private String email;

    // PENDING, ACCEPTED, DECLINED
    private InviteStatus status;

    private LocalDateTime createdAt;

    // ↓ 받은 초대 목록(sp_my_invite_list)에서만 채워져요 (조인해 온 값)
    private String workspaceName;

    private String workspaceInitials;

    private WorkspaceColor workspaceColor;

    private String inviterName;
}
