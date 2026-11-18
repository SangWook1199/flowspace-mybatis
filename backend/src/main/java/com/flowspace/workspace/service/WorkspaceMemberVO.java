package com.flowspace.workspace.service;

import java.time.LocalDateTime;

import com.flowspace.entity.enums.WorkspaceRole;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 워크스페이스 멤버 한 명을 담는 객체예요(프로시저 sp_workspace_member_list, sp_workspace_member의 결과).
// workspace_members, users, files 세 테이블을 조인한 결과예요.
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceMemberVO {

    private Long workspaceId;

    private Long userId;

    private String nickname;

    private String email;

    private String bio;

    private Long profileFileId;

    private String profileImageUrl;

    private LocalDateTime lastActiveAt;

    // 이 워크스페이스에서의 역할 (OWNER, ADMIN, MEMBER)
    private WorkspaceRole role;

    private LocalDateTime joinedAt;
}
