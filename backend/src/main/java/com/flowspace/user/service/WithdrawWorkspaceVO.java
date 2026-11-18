package com.flowspace.user.service;

import com.flowspace.entity.enums.WorkspaceRole;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 탈퇴 전 확인에서 쓰는, 내가 속한 워크스페이스 한 곳이에요(프로시저 sp_withdraw_check의 결과).
@Getter
@Setter
@NoArgsConstructor
public class WithdrawWorkspaceVO {

    private Long workspaceId;

    private String name;

    // 소유자에게 "멤버가 탈퇴했다"고 알릴 때 써요
    private Long ownerId;

    // 이 워크스페이스에서 내 역할
    private WorkspaceRole role;

    // 나를 포함한 멤버 수
    private int memberCount;
}
