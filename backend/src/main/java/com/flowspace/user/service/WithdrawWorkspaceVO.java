package com.flowspace.user.service;

import com.flowspace.common.enums.WorkspaceRole;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 탈퇴 전 확인용 소속 워크스페이스 (sp_withdraw_check 결과)
@Getter
@Setter
@NoArgsConstructor
public class WithdrawWorkspaceVO {

    private Long workspaceId;

    private String name;

    // 소유자에게 멤버 탈퇴 알림용
    private Long ownerId;

    // 이 워크스페이스에서 내 역할
    private WorkspaceRole role;

    // 나를 포함한 멤버 수
    private int memberCount;
}
