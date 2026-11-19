package com.flowspace.service;

import com.flowspace.notification.service.NotificationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flowspace.entity.User;
import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.repository.UserRepository;
import com.flowspace.repository.WorkspaceRepository;
import com.flowspace.workspace.dto.request.WorkspaceInviteRequest;
import com.flowspace.workspace.dto.request.WorkspaceRoleChangeRequest;
import com.flowspace.workspace.dto.response.WorkspaceMemberResponse;
import com.flowspace.workspace.service.InviteVO;
import com.flowspace.workspace.service.WorkspaceMemberVO;
import com.flowspace.workspace.service.WorkspaceVO;
import com.flowspace.workspace.service.impl.WorkspaceMapper;
import com.flowspace.workspace.service.impl.WorkspaceServiceImpl;

// 워크스페이스 역할 규칙: 초대·추방은 관리자 이상, 역할 변경은 소유자, 관리자는 관리자 추방 불가
// 역할은 Mapper(selectMemberRole) 반환 값으로 지정
@ExtendWith(MockitoExtension.class)
class WorkspaceServiceRoleTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PresenceService presenceService;
    @Mock
    private WorkspaceMapper workspaceMapper;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private User actor;
    private User target;

    @BeforeEach
    void setUp() {
        actor = User.builder().userId(1L).email("actor@a.com").nickname("actor").build();
        target = User.builder().userId(2L).email("target@a.com").nickname("target").build();

        lenient().when(userRepository.findByEmail("actor@a.com")).thenReturn(Optional.of(actor));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        lenient().when(userRepository.existsById(2L)).thenReturn(true);

        WorkspaceVO workspace = new WorkspaceVO();
        workspace.setWorkspaceId(5L);
        workspace.setName("w");

        lenient().when(workspaceMapper.selectWorkspaceById(5L)).thenReturn(workspace);
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
        lenient().when(userRepository.findByEmail("target@a.com")).thenReturn(Optional.of(target));

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
