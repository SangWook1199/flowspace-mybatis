package com.flowspace.workspace.service.impl;

import com.flowspace.notification.service.NotificationService;
import com.flowspace.presence.service.PresenceService;
import com.flowspace.workspace.service.InviteVO;
import com.flowspace.workspace.service.MemberTaskCountVO;
import com.flowspace.workspace.service.WorkspaceMemberVO;
import com.flowspace.workspace.service.WorkspaceService;
import com.flowspace.workspace.service.WorkspaceVO;
import com.flowspace.user.service.UserVO;

import com.flowspace.workspace.dto.request.*;
import com.flowspace.workspace.dto.response.*;
import com.flowspace.entity.enums.*;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceServiceImpl implements WorkspaceService {

        // 프로필 카드에 보여줄 담당 작업 최대 개수
        private static final int PROFILE_TASK_LIMIT = 5;

        private final NotificationService notificationService;
        private final PresenceService presenceService;

        // 워크스페이스 SQL
        private final WorkspaceMapper workspaceMapper;

        // 워크스페이스 생성
        @Override
        public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request, String email) {

                UserVO user = findUser(email);

                WorkspaceVO workspace = new WorkspaceVO();
                workspace.setName(request.name());
                workspace.setInitials(request.initials());
                workspace.setColor(request.color());
                workspace.setIcon(normalizeIcon(request.icon()));

                saveNewWorkspace(workspace, user.getUserId());

                return WorkspaceResponse.from(workspace, WorkspaceRole.OWNER);
        }

        // 아이콘 값 정리 (비어 있으면 null, 아니면 앞뒤 공백 제거)
        private String normalizeIcon(String icon) {
                return icon == null || icon.isBlank() ? null : icon.trim();
        }

        // 워크스페이스 수정 (소유자만 가능)
        @Override
        public WorkspaceResponse updateWorkspace(Long workspaceId, WorkspaceUpdateRequest request, String email) {

                UserVO user = findUser(email);

                WorkspaceVO workspace = findWorkspace(workspaceId);

                WorkspaceRole role = findMemberRole(workspaceId, user);

                if (role != WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                workspace.setName(request.name().trim());
                workspace.setInitials(request.initials().trim());
                workspace.setColor(request.color());
                workspace.setIcon(normalizeIcon(request.icon()));

                workspaceMapper.updateWorkspace(workspace);

                return WorkspaceResponse.from(workspace, role);
        }

        // 개인 워크스페이스 생성 (회원가입 전용, 워크스페이스 번호 반환)
        @Override
        public Long createPersonalWorkspace(Long userId, String nickname) {

                WorkspaceVO workspace = new WorkspaceVO();
                workspace.setName(nickname + "의 워크스페이스");
                workspace.setInitials(nickname.substring(0, 1));
                workspace.setColor(WorkspaceColor.BLUE);

                saveNewWorkspace(workspace, userId);

                return workspace.getWorkspaceId();
        }

        // 새 워크스페이스 저장 공통 처리: 워크스페이스 INSERT → 소유자·기본 상태 연결 → 마지막으로 본 워크스페이스로 지정
        private void saveNewWorkspace(WorkspaceVO workspace, Long ownerId) {

                workspace.setOwnerId(ownerId);

                workspaceMapper.insertWorkspace(workspace);

                workspaceMapper.insertWorkspaceInit(workspace.getWorkspaceId(), ownerId);

                workspaceMapper.updateUserLastWorkspace(ownerId, workspace.getWorkspaceId());
        }

        // 내 워크스페이스 목록 조회
        @Transactional(readOnly = true)
        @Override
        public List<WorkspaceResponse> getMyWorkspaces(String email) {

                UserVO user = findUser(email);

                return workspaceMapper.selectMyWorkspaceList(user.getUserId()).stream()
                        .map(workspace -> WorkspaceResponse.from(workspace, workspace.getRole())).toList();
        }

        // 워크스페이스 단건 조회
        @Transactional(readOnly = true)
        @Override
        public WorkspaceResponse getWorkspace(Long workspaceId, String email) {

                UserVO user = findUser(email);

                WorkspaceVO workspace = findWorkspace(workspaceId);

                WorkspaceRole role = findMemberRole(workspaceId, user);

                return WorkspaceResponse.from(workspace, role);
        }

        // 워크스페이스 멤버 초대
        @Override
        public WorkspaceInviteResponse inviteMember(Long workspaceId, WorkspaceInviteRequest request, String email) {

                UserVO inviter = findUser(email);

                WorkspaceVO workspace = findWorkspace(workspaceId);

                if (!findMemberRole(workspaceId, inviter).isAtLeast(WorkspaceRole.ADMIN)) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                UserVO invitee = findUser(request.email());

                if (workspaceMapper.selectMemberRole(workspaceId, invitee.getUserId()) != null) {
                        throw new FlowSpaceException(ErrorCode.ALREADY_WORKSPACE_MEMBER);
                }

                if (workspaceMapper.selectPendingInviteCount(workspaceId, request.email()) > 0) {
                        throw new FlowSpaceException(ErrorCode.ALREADY_INVITED);
                }

                InviteVO invite = new InviteVO();
                invite.setWorkspaceId(workspaceId);
                invite.setInviterId(inviter.getUserId());
                invite.setEmail(request.email());
                invite.setStatus(InviteStatus.PENDING);

                workspaceMapper.insertInvite(invite);

                notificationService.send(invitee.getUserId(), inviter.getUserId(), workspaceId,
                        NotificationType.WORKSPACE_INVITE,
                        inviter.getNickname() + "님이 '" + workspace.getName() + "' 워크스페이스에 초대했어요.",
                        invite.getInviteId(), null);

                return WorkspaceInviteResponse.from(invite);
        }

        // 내가 받은 초대 목록 조회
        @Transactional(readOnly = true)
        @Override
        public List<InviteResponse> getMyInvites(String email) {

                UserVO user = findUser(email);

                return workspaceMapper.selectMyInviteList(user.getEmail()).stream().map(InviteResponse::from).toList();
        }

        // 워크스페이스 초대 수락
        @Override
        public void acceptInvite(Long inviteId, String email) {

                UserVO user = findUser(email);

                InviteVO invite = findMyPendingInvite(inviteId, user);

                workspaceMapper.insertWorkspaceMember(invite.getWorkspaceId(), user.getUserId(), WorkspaceRole.MEMBER);
                workspaceMapper.updateInviteStatus(inviteId, InviteStatus.ACCEPTED);

                workspaceMapper.updateUserLastWorkspace(user.getUserId(), invite.getWorkspaceId());

                notificationService.markReadByRef(user.getUserId(), NotificationType.WORKSPACE_INVITE, inviteId);
                notificationService.send(invite.getInviterId(), user.getUserId(), invite.getWorkspaceId(),
                        NotificationType.INVITE_ACCEPTED,
                        user.getNickname() + "님이 '" + findWorkspace(invite.getWorkspaceId()).getName()
                                + "' 초대를 수락했어요.",
                        inviteId, null);
        }

        // 워크스페이스 초대 거절
        @Override
        public void declineInvite(Long inviteId, String email) {

                UserVO user = findUser(email);

                InviteVO invite = findMyPendingInvite(inviteId, user);

                workspaceMapper.updateInviteStatus(inviteId, InviteStatus.DECLINED);

                notificationService.markReadByRef(user.getUserId(), NotificationType.WORKSPACE_INVITE, inviteId);
                notificationService.send(invite.getInviterId(), user.getUserId(), invite.getWorkspaceId(),
                        NotificationType.INVITE_DECLINED,
                        user.getNickname() + "님이 '" + findWorkspace(invite.getWorkspaceId()).getName()
                                + "' 초대를 거절했어요.",
                        inviteId, null);
        }

        // 나에게 온, 아직 답하지 않은 초대 (아니면 예외)
        private InviteVO findMyPendingInvite(Long inviteId, UserVO user) {

                InviteVO invite = workspaceMapper.selectInviteById(inviteId);

                if (invite == null) {
                        throw new FlowSpaceException(ErrorCode.INVITE_NOT_FOUND);
                }

                if (!invite.getEmail().equals(user.getEmail())) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                if (invite.getStatus() != InviteStatus.PENDING) {
                        throw new FlowSpaceException(ErrorCode.INVALID_INVITE_STATUS);
                }

                return invite;
        }

        // 워크스페이스 멤버 목록 조회
        @Transactional(readOnly = true)
        @Override
        public List<WorkspaceMemberResponse> getMembers(Long workspaceId, String email) {

                UserVO user = findUser(email);

                findWorkspace(workspaceId);
                findMemberRole(workspaceId, user);

                return workspaceMapper.selectWorkspaceMemberList(workspaceId).stream()
                        .map(member -> WorkspaceMemberResponse.from(member, presenceService.isOnline(member.getUserId())))
                        .toList();
        }

        // 멤버 프로필 카드 조회 (같은 워크스페이스 멤버만, 해당 워크스페이스 정보만)
        @Transactional(readOnly = true)
        @Override
        public MemberProfileResponse getMemberProfile(Long workspaceId, Long userId, String email) {

                UserVO viewer = findUser(email);

                findWorkspace(workspaceId);
                findMemberRole(workspaceId, viewer);

                if (workspaceMapper.selectUserById(userId) == null) {
                        throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
                }

                WorkspaceMemberVO member = workspaceMapper.selectWorkspaceMember(workspaceId, userId);

                if (member == null) {
                        throw new FlowSpaceException(ErrorCode.MEMBER_NOT_FOUND);
                }

                List<MemberProfileResponse.TaskItem> tasks = workspaceMapper
                        .selectMemberOpenTaskList(workspaceId, userId, PROFILE_TASK_LIMIT).stream()
                        .map(MemberProfileResponse.TaskItem::from).toList();

                MemberTaskCountVO count = workspaceMapper.selectMemberTaskCount(workspaceId, userId);

                return new MemberProfileResponse(member.getUserId(), member.getNickname(), member.getEmail(),
                        member.getBio(), member.getProfileImageUrl(), member.getRole(), member.getJoinedAt(),
                        presenceService.isOnline(userId), member.getLastActiveAt(), count.getOpenTaskCount(),
                        count.getDoneTaskCount(), tasks);
        }

        // 워크스페이스 소유권 이전
        @Override
        public void transferOwnership(Long workspaceId, WorkspaceOwnerTransferRequest request, String email) {

                UserVO user = findUser(email);

                WorkspaceVO workspace = findWorkspace(workspaceId);

                if (findMemberRole(workspaceId, user) != WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                UserVO targetUser = findUserById(request.userId());

                if (workspaceMapper.selectMemberRole(workspaceId, targetUser.getUserId()) == null) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                workspaceMapper.updateMemberRole(workspaceId, user.getUserId(), WorkspaceRole.MEMBER);
                workspaceMapper.updateMemberRole(workspaceId, targetUser.getUserId(), WorkspaceRole.OWNER);
                workspaceMapper.updateWorkspaceOwner(workspaceId, targetUser.getUserId());

                notificationService.send(targetUser.getUserId(), user.getUserId(), workspaceId,
                        NotificationType.OWNERSHIP_TRANSFERRED,
                        user.getNickname() + "님이 '" + workspace.getName() + "'의 소유권을 넘겼어요. 이제 내가 소유자예요.", null,
                        null);
        }

        // 워크스페이스 삭제
        @Override
        public void deleteWorkspace(Long workspaceId, String email) {

                UserVO user = findUser(email);

                findWorkspace(workspaceId);

                if (findMemberRole(workspaceId, user) != WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                if (workspaceMapper.selectMyWorkspaceCount(user.getUserId()) <= 1) {
                        throw new FlowSpaceException(ErrorCode.LAST_WORKSPACE);
                }

                workspaceMapper.deleteWorkspace(workspaceId);
        }

        // 워크스페이스 멤버 추방 (관리자는 일반 멤버만, 소유자는 관리자·멤버 추방 가능)
        @Override
        public void removeMember(Long workspaceId, Long userId, String email) {

                UserVO loginUser = findUser(email);

                WorkspaceVO workspace = findWorkspace(workspaceId);

                WorkspaceRole actorRole = findMemberRole(workspaceId, loginUser);

                if (!actorRole.isAtLeast(WorkspaceRole.ADMIN)) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                UserVO targetUser = findUserById(userId);

                WorkspaceRole targetRole = workspaceMapper.selectMemberRole(workspaceId, userId);

                if (targetRole == null) {
                        throw new FlowSpaceException(ErrorCode.MEMBER_NOT_FOUND);
                }

                if (targetRole == WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.OWNER_CANNOT_REMOVE);
                }

                if (actorRole == WorkspaceRole.ADMIN && targetRole != WorkspaceRole.MEMBER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                workspaceMapper.deleteWorkspaceMember(workspaceId, userId);

                notificationService.send(targetUser.getUserId(), loginUser.getUserId(), workspaceId,
                        NotificationType.MEMBER_REMOVED, "'" + workspace.getName() + "' 워크스페이스에서 내보내졌어요.", null,
                        null);
        }

        // 멤버 역할 변경 (소유자만 가능, ADMIN ↔ MEMBER)
        @Override
        public WorkspaceMemberResponse changeMemberRole(Long workspaceId, Long userId,
                WorkspaceRoleChangeRequest request, String email) {

                UserVO loginUser = findUser(email);

                findWorkspace(workspaceId);

                if (findMemberRole(workspaceId, loginUser) != WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                if (request.role() == WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.INVALID_ROLE_CHANGE);
                }

                if (workspaceMapper.selectUserById(userId) == null) {
                        throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
                }

                WorkspaceMemberVO member = workspaceMapper.selectWorkspaceMember(workspaceId, userId);

                if (member == null) {
                        throw new FlowSpaceException(ErrorCode.MEMBER_NOT_FOUND);
                }

                if (member.getRole() == WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.INVALID_ROLE_CHANGE);
                }

                workspaceMapper.updateMemberRole(workspaceId, userId, request.role());

                member.setRole(request.role());

                return WorkspaceMemberResponse.from(member, presenceService.isOnline(userId));
        }

        // 워크스페이스 나가기
        @Override
        public void leaveWorkspace(Long workspaceId, String email) {

                UserVO user = findUser(email);

                WorkspaceVO workspace = findWorkspace(workspaceId);

                if (findMemberRole(workspaceId, user) == WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.OWNER_CANNOT_LEAVE);
                }

                if (workspaceMapper.selectMyWorkspaceCount(user.getUserId()) <= 1) {
                        throw new FlowSpaceException(ErrorCode.LAST_WORKSPACE);
                }

                workspaceMapper.deleteWorkspaceMember(workspaceId, user.getUserId());

                notificationService.send(workspace.getOwnerId(), user.getUserId(), workspaceId,
                        NotificationType.MEMBER_LEFT,
                        user.getNickname() + "님이 '" + workspace.getName() + "' 워크스페이스를 나갔어요.", null, null);

                workspaceMapper.clearUserLastWorkspace(user.getUserId(), workspaceId);
        }

        // 이메일로 사용자 (없으면 예외)
        private UserVO findUser(String email) {

                UserVO user = workspaceMapper.selectUserByEmail(email);

                if (user == null) {
                        throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
                }

                return user;
        }

        // 번호로 사용자 (없으면 예외)
        private UserVO findUserById(Long userId) {

                UserVO user = workspaceMapper.selectUserById(userId);

                if (user == null) {
                        throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
                }

                return user;
        }

        // 워크스페이스 한 건 (없으면 예외)
        private WorkspaceVO findWorkspace(Long workspaceId) {

                WorkspaceVO workspace = workspaceMapper.selectWorkspaceById(workspaceId);

                if (workspace == null) {
                        throw new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND);
                }

                return workspace;
        }

        // 로그인한 사람의 그 워크스페이스 역할 (멤버가 아니면 예외)
        private WorkspaceRole findMemberRole(Long workspaceId, UserVO user) {

                WorkspaceRole role = workspaceMapper.selectMemberRole(workspaceId, user.getUserId());

                if (role == null) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                return role;
        }
}
