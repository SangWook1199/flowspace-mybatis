package com.flowspace.user.dto.response;

import java.util.List;

// @formatter:off

// 탈퇴 전 워크스페이스 처리 안내 응답 DTO
// - blocking: 다른 멤버가 있는 내 워크스페이스 (소유권 이전 또는 멤버 내보내기 필요)
// - deleting: 나만 있는 내 워크스페이스 (탈퇴 시 삭제)
// - leaving: 멤버로 참여 중인 워크스페이스 (탈퇴 시 자동 나가기)
public record WithdrawCheckResponse(

    boolean canWithdraw,
    List<WorkspaceBrief> blocking,
    List<WorkspaceBrief> deleting,
    List<WorkspaceBrief> leaving

) {

    public record WorkspaceBrief(

        Long workspaceId,
        String name,
        int memberCount

    ) {}
}

// @formatter:on
