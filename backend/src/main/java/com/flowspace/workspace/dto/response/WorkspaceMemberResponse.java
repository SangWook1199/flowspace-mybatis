package com.flowspace.workspace.dto.response;

import java.time.LocalDateTime;

import com.flowspace.common.enums.WorkspaceRole;
import com.flowspace.workspace.service.WorkspaceMemberVO;

// @formatter:off

// 워크스페이스 멤버 응답 DTO (online: 지금 접속 중인지, lastActiveAt: 마지막으로 접속해 있던 시각 — 없으면 null)
public record WorkspaceMemberResponse(

    Long userId,
    String name,
    String nickname,
    Long profileFileId,
    String profileImageUrl,
    WorkspaceRole role,
    boolean online,
    LocalDateTime lastActiveAt

) {

    // WorkspaceMemberVO 기반 응답 생성
    public static WorkspaceMemberResponse from(WorkspaceMemberVO member, boolean online) {
        return new WorkspaceMemberResponse(
            member.getUserId(),
            member.getNickname(),
            member.getNickname(),
            member.getProfileFileId(),
            member.getProfileImageUrl(),
            member.getRole(),
            online,
            member.getLastActiveAt()
        );
    }
}

// @formatter:on