package com.flowspace.workspace.service.impl;

import com.flowspace.presence.service.PresenceService;

import com.flowspace.notification.service.NotificationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flowspace.common.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.workspace.dto.request.WorkspaceInviteRequest;
import com.flowspace.workspace.dto.request.WorkspaceRoleChangeRequest;
import com.flowspace.workspace.dto.response.WorkspaceMemberResponse;
import com.flowspace.user.service.UserVO;
import com.flowspace.workspace.service.InviteVO;
import com.flowspace.workspace.service.WorkspaceMemberVO;
import com.flowspace.workspace.service.WorkspaceVO;

// 워크스페이스 역할 규칙: 초대·추방은 관리자 이상, 역할 변경은 소유자, 관리자는 관리자 추방 불가
// 역할은 Mapper(selectMemberRole) 반환 값으로 지정
@ExtendWith(MockitoExtension.class)
class WorkspaceServiceRoleTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private PresenceService presenceService;
    @Mock
    private WorkspaceMapper workspaceMapper;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private UserVO actor;
    private UserVO target;

    @BeforeEach
    void setUp() {
        actor = user(1L, "actor@a.com", "actor");
        target = user(2L, "target@a.com", "target");

        lenient().when(workspaceMapper.selectUserByEmail("actor@a.com")).thenReturn(actor);
        lenient().when(workspaceMapper.selectUserById(2L)).thenReturn(target);

        WorkspaceVO workspace = new WorkspaceVO();
        workspace.setWorkspaceId(5L);
        workspace.setName("w");

        lenient().when(workspaceMapper.selectWorkspaceById(5L)).thenReturn(workspace);
    }

    // 사용자 한 명 (MyBatis 조회 결과)
    private UserVO user(Long userId, String email, String nickname) {
        UserVO user = new UserVO();
        user.setUserId(userId);
        user.setEmail(email);
        user.setNickname(nickname);
        return user;
    }

    // 로그인한 사람(1번)의 역할
    private void actorRole(WorkspaceRole role) {
        lenient().when(workspaceMapper.selectMemberRole(5L, 1L)).thenReturn(role);
    }

    // 대상(2번)의 역할 (역할 변경은 멤버 정보 전체를 조회)
    private void targetRole(WorkspaceRole role) {
        lenient().when(workspaceMapper.selectMemberRole(5L, 2L)).thenReturn(role);

        WorkspaceMemberVO member = new WorkspaceMemberVO();
        member.setWorkspaceId(5L);
        member.setUserId(2L);
        member.setNickname("target");
        member.setRole(role);

        lenient().when(workspaceMapper.selectWorkspaceMember(5L, 2L)).thenReturn(member);
    }

    // ───── 초대 ─────

    @Test
    @DisplayName("일반 멤버는 초대할 수 없다")
    void memberCannotInvite() {
        actorRole(WorkspaceRole.MEMBER);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> workspaceService.inviteMember(5L, new WorkspaceInviteRequest("target@a.com"), "actor@a.com"));

        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(workspaceMapper, never()).insertInvite(any());
    }

    @Test
    @DisplayName("관리자는 초대할 수 있다")
    void adminCanInvite() {
        actorRole(WorkspaceRole.ADMIN);
        lenient().when(workspaceMapper.selectUserByEmail("target@a.com")).thenReturn(target);

        workspaceService.inviteMember(5L, new WorkspaceInviteRequest("target@a.com"), "actor@a.com");

        verify(workspaceMapper).insertInvite(any(InviteVO.class));
    }

    // ───── 추방 ─────

    @Test
    @DisplayName("일반 멤버는 추방할 수 없다")
    void memberCannotRemove() {
        actorRole(WorkspaceRole.MEMBER);
        targetRole(WorkspaceRole.MEMBER);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> workspaceService.removeMember(5L, 2L, "actor@a.com"));

        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(workspaceMapper, never()).deleteWorkspaceMember(any(), any());
    }

    @Test
    @DisplayName("관리자는 일반 멤버를 내보낼 수 있다")
    void adminCanRemoveMember() {
        actorRole(WorkspaceRole.ADMIN);
        targetRole(WorkspaceRole.MEMBER);

        workspaceService.removeMember(5L, 2L, "actor@a.com");

        verify(workspaceMapper).deleteWorkspaceMember(5L, 2L);
    }

    @Test
    @DisplayName("관리자는 다른 관리자를 내보낼 수 없다")
    void adminCannotRemoveAdmin() {
        actorRole(WorkspaceRole.ADMIN);
        targetRole(WorkspaceRole.ADMIN);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> workspaceService.removeMember(5L, 2L, "actor@a.com"));

        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(workspaceMapper, never()).deleteWorkspaceMember(any(), any());
    }

    @Test
    @DisplayName("소유자는 관리자도 내보낼 수 있다")
    void ownerCanRemoveAdmin() {
        actorRole(WorkspaceRole.OWNER);
        targetRole(WorkspaceRole.ADMIN);

        workspaceService.removeMember(5L, 2L, "actor@a.com");

        verify(workspaceMapper).deleteWorkspaceMember(5L, 2L);
    }

    @Test
    @DisplayName("소유자는 누구도 내보낼 수 없다")
    void ownerCannotBeRemoved() {
        actorRole(WorkspaceRole.ADMIN);
        targetRole(WorkspaceRole.OWNER);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> workspaceService.removeMember(5L, 2L, "actor@a.com"));

        assertEquals(ErrorCode.OWNER_CANNOT_REMOVE, e.getErrorCode());
        verify(workspaceMapper, never()).deleteWorkspaceMember(any(), any());
    }

    // ───── 역할 변경 ─────

    @Test
    @DisplayName("소유자는 멤버를 관리자로 바꿀 수 있다")
    void ownerCanPromoteMember() {
        actorRole(WorkspaceRole.OWNER);
        targetRole(WorkspaceRole.MEMBER);

        WorkspaceMemberResponse response = workspaceService.changeMemberRole(5L, 2L,
            new WorkspaceRoleChangeRequest(WorkspaceRole.ADMIN), "actor@a.com");

        verify(workspaceMapper).updateMemberRole(5L, 2L, WorkspaceRole.ADMIN);
        assertEquals(WorkspaceRole.ADMIN, response.role());
    }

    @Test
    @DisplayName("소유자는 관리자를 다시 멤버로 바꿀 수 있다")
    void ownerCanDemoteAdmin() {
        actorRole(WorkspaceRole.OWNER);
        targetRole(WorkspaceRole.ADMIN);

        workspaceService.changeMemberRole(5L, 2L, new WorkspaceRoleChangeRequest(WorkspaceRole.MEMBER),
            "actor@a.com");

        verify(workspaceMapper).updateMemberRole(5L, 2L, WorkspaceRole.MEMBER);
    }

    @Test
    @DisplayName("관리자는 역할을 바꿀 수 없다")
    void adminCannotChangeRole() {
        actorRole(WorkspaceRole.ADMIN);
        targetRole(WorkspaceRole.MEMBER);

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> workspaceService.changeMemberRole(5L, 2L,
            new WorkspaceRoleChangeRequest(WorkspaceRole.ADMIN), "actor@a.com"));

        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(workspaceMapper, never()).updateMemberRole(any(), any(), any());
    }

    @Test
    @DisplayName("역할을 소유자로 바꾸는 요청은 거절한다 (소유권 이전을 써야 해요)")
    void cannotSetOwnerByRoleChange() {
        actorRole(WorkspaceRole.OWNER);
        targetRole(WorkspaceRole.MEMBER);

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> workspaceService.changeMemberRole(5L, 2L,
            new WorkspaceRoleChangeRequest(WorkspaceRole.OWNER), "actor@a.com"));

        assertEquals(ErrorCode.INVALID_ROLE_CHANGE, e.getErrorCode());
        verify(workspaceMapper, never()).updateMemberRole(any(), any(), any());
    }

    @Test
    @DisplayName("소유자의 역할은 바꿀 수 없다")
    void cannotChangeOwnerRole() {
        actorRole(WorkspaceRole.OWNER);
        targetRole(WorkspaceRole.OWNER);

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> workspaceService.changeMemberRole(5L, 2L,
            new WorkspaceRoleChangeRequest(WorkspaceRole.MEMBER), "actor@a.com"));

        assertEquals(ErrorCode.INVALID_ROLE_CHANGE, e.getErrorCode());
        verify(workspaceMapper, never()).updateMemberRole(any(), any(), any());
    }
}
