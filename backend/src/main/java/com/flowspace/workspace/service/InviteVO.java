package com.flowspace.workspace.service;

import java.time.LocalDateTime;

import com.flowspace.entity.enums.InviteStatus;
import com.flowspace.entity.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 워크스페이스 초대 (등록·조회 공용)
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

    // ↓ 받은 초대 목록(sp_my_invite_list) 전용 (조인 컬럼)
    private String workspaceName;

    private String workspaceInitials;

    private WorkspaceColor workspaceColor;

    private String inviterName;
}
