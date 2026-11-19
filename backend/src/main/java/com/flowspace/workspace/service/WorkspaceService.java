package com.flowspace.workspace.service;

import java.util.List;

import com.flowspace.workspace.dto.request.WorkspaceCreateRequest;
import com.flowspace.workspace.dto.request.WorkspaceInviteRequest;
import com.flowspace.workspace.dto.request.WorkspaceOwnerTransferRequest;
import com.flowspace.workspace.dto.request.WorkspaceRoleChangeRequest;
import com.flowspace.workspace.dto.request.WorkspaceUpdateRequest;
import com.flowspace.workspace.dto.response.InviteResponse;
import com.flowspace.workspace.dto.response.MemberProfileResponse;
import com.flowspace.workspace.dto.response.WorkspaceInviteResponse;
import com.flowspace.workspace.dto.response.WorkspaceMemberResponse;
import com.flowspace.workspace.dto.response.WorkspaceResponse;

// 워크스페이스 서비스
public interface WorkspaceService {

    // 워크스페이스 생성
    WorkspaceResponse createWorkspace(WorkspaceCreateRequest request, String email);

    // 워크스페이스 수정 (소유자만 가능)
    WorkspaceResponse updateWorkspace(Long workspaceId, WorkspaceUpdateRequest request, String email);

    // 개인 워크스페이스 생성 (회원가입 전용)
    Long createPersonalWorkspace(Long userId, String nickname);

    // 내 워크스페이스 목록 조회
    List<WorkspaceResponse> getMyWorkspaces(String email);

    // 워크스페이스 단건 조회
    WorkspaceResponse getWorkspace(Long workspaceId, String email);

    // 워크스페이스 멤버 초대
    WorkspaceInviteResponse inviteMember(Long workspaceId, WorkspaceInviteRequest request, String email);

    // 내가 받은 초대 목록 조회
    List<InviteResponse> getMyInvites(String email);

    // 워크스페이스 초대 수락
    void acceptInvite(Long inviteId, String email);

    // 워크스페이스 초대 거절
    void declineInvite(Long inviteId, String email);

    // 워크스페이스 멤버 목록 조회
    List<WorkspaceMemberResponse> getMembers(Long workspaceId, String email);

    // 멤버 프로필 카드 조회 (같은 워크스페이스 멤버만, 해당 워크스페이스 정보만)
    MemberProfileResponse getMemberProfile(Long workspaceId, Long userId, String email);

    // 워크스페이스 소유권 이전
    void transferOwnership(Long workspaceId, WorkspaceOwnerTransferRequest request, String email);

    // 워크스페이스 삭제
    void deleteWorkspace(Long workspaceId, String email);

    // 워크스페이스 멤버 추방 (관리자는 일반 멤버만, 소유자는 관리자·멤버 추방 가능)
    void removeMember(Long workspaceId, Long userId, String email);

    // 멤버 역할 변경 (소유자만 가능, ADMIN ↔ MEMBER)
    WorkspaceMemberResponse changeMemberRole(Long workspaceId, Long userId, WorkspaceRoleChangeRequest request, String email);

    // 워크스페이스 나가기
    void leaveWorkspace(Long workspaceId, String email);
}
