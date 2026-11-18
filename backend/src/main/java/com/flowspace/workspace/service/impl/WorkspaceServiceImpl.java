package com.flowspace.workspace.service.impl;

import com.flowspace.service.NotificationService;
import com.flowspace.service.PresenceService;
import com.flowspace.workspace.service.InviteVO;
import com.flowspace.workspace.service.MemberTaskCountVO;
import com.flowspace.workspace.service.WorkspaceMemberVO;
import com.flowspace.workspace.service.WorkspaceService;
import com.flowspace.workspace.service.WorkspaceVO;

import com.flowspace.workspace.dto.request.*;
import com.flowspace.workspace.dto.response.*;
import com.flowspace.entity.*;
import com.flowspace.entity.enums.*;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.repository.*;
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

        private final WorkspaceRepository workspaceRepository;
        private final UserRepository userRepository;
        private final NotificationService notificationService;
        private final PresenceService presenceService;

        // 워크스페이스 조회용 MyBatis Mapper (같은 패키지라 import가 없어요)
        private final WorkspaceMapper workspaceMapper;

        // 워크스페이스 생성
        @Override
        public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request, String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                WorkspaceVO workspace = new WorkspaceVO();
                workspace.setName(request.name());
                workspace.setInitials(request.initials());
                workspace.setColor(request.color());
                workspace.setIcon(normalizeIcon(request.icon()));

                saveNewWorkspace(workspace, user.getUserId());

                return WorkspaceResponse.from(workspace, WorkspaceRole.OWNER);
        }

        // 아이콘 값 정리: 비어 있으면 null(아이콘 없음), 아니면 앞뒤 공백을 지워요.
        private String normalizeIcon(String icon) {
                return icon == null || icon.isBlank() ? null : icon.trim();
        }

        // 워크스페이스 수정 (소유자만 가능)
        @Override
        public WorkspaceResponse updateWorkspace(Long workspaceId, WorkspaceUpdateRequest request, String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                WorkspaceVO workspace = findWorkspace(workspaceId);

                WorkspaceRole role = findMemberRole(workspaceId, user);

                if (role != WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                workspace.setName(request.name().trim());
                workspace.setInitials(request.initials().trim());
                workspace.setColor(request.color());
                workspace.setIcon(normalizeIcon(request.icon()));

                // JPA는 값만 바꾸면 끝날 때 알아서 UPDATE 했지만, MyBatis는 직접 호출해야 DB에 반영돼요
                workspaceMapper.updateWorkspace(workspace);

                return WorkspaceResponse.from(workspace, role);
        }

        // 개인 워크스페이스 생성 (회원가입 전용). 새 워크스페이스 번호를 돌려줘요.
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

                // INSERT가 끝나면 DB가 만든 번호가 workspace.getWorkspaceId()에 들어 있어요
                workspaceMapper.insertWorkspace(workspace);

                // 소유자 멤버와 기본 상태 3개를 프로시저 한 번으로 연결해요
                workspaceMapper.insertWorkspaceInit(workspace.getWorkspaceId(), ownerId);

                // 이제 회원가입(AuthService)이 사용자 엔티티를 들고 바꾸지 않아서 SQL로 바꿔도 덮어쓰이지 않아요.
                // (워크스페이스 생성에서 읽는 사용자 엔티티도 값을 바꾸지 않아서 JPA가 다시 쓰지 않아요.)
                workspaceMapper.updateUserLastWorkspace(ownerId, workspace.getWorkspaceId());
        }

        // 내 워크스페이스 목록 조회
        @Transactional(readOnly = true)
        @Override
        public List<WorkspaceResponse> getMyWorkspaces(String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                // 워크스페이스와 내 역할을 프로시저 한 번으로 가져와요
                return workspaceMapper.selectMyWorkspaceList(user.getUserId()).stream()
                        .map(workspace -> WorkspaceResponse.from(workspace, workspace.getRole())).toList();
        }

        // 워크스페이스 단건 조회
        @Transactional(readOnly = true)
        @Override
        public WorkspaceResponse getWorkspace(Long workspaceId, String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                WorkspaceVO workspace = findWorkspace(workspaceId);

                // 테이블 하나씩만 읽으면 돼서 프로시저 없이 XML SQL 두 번으로 해요
                WorkspaceRole role = findMemberRole(workspaceId, user);

                return WorkspaceResponse.from(workspace, role);
        }

        // 워크스페이스 멤버 초대
        @Override
        public WorkspaceInviteResponse inviteMember(Long workspaceId, WorkspaceInviteRequest request, String email) {

                User inviter = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                WorkspaceVO workspace = findWorkspace(workspaceId);

                // 멤버 초대는 관리자 이상만 할 수 있어요.
                if (!findMemberRole(workspaceId, inviter).isAtLeast(WorkspaceRole.ADMIN)) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                User invitee = userRepository.findByEmail(request.email())
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                // 멤버 확인 SQL을 그대로 써요: 역할이 있으면 이미 멤버예요
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

                // 초대받은 사람에게 알림 (알림창에서 바로 수락/거절할 수 있어요)
                // 알림 기능은 아직 JPA라서 워크스페이스를 번호만 가진 엔티티로 넘겨요
                notificationService.send(invitee, inviter, workspaceRepository.getReferenceById(workspaceId),
                        NotificationType.WORKSPACE_INVITE,
                        inviter.getNickname() + "님이 '" + workspace.getName() + "' 워크스페이스에 초대했어요.",
                        invite.getInviteId(), null);

                return WorkspaceInviteResponse.from(invite);
        }

        // 내가 받은 초대 목록 조회
        @Transactional(readOnly = true)
        @Override
        public List<InviteResponse> getMyInvites(String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                return workspaceMapper.selectMyInviteList(user.getEmail()).stream().map(InviteResponse::from).toList();
        }

        // 워크스페이스 초대 수락
        @Override
        public void acceptInvite(Long inviteId, String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                InviteVO invite = findMyPendingInvite(inviteId, user);

                // 멤버 추가와 초대 상태 변경은 각각 테이블 하나라서 XML SQL 두 번이에요.
                // 같은 트랜잭션이라 둘 중 하나가 실패하면 둘 다 취소돼요.
                workspaceMapper.insertWorkspaceMember(invite.getWorkspaceId(), user.getUserId(), WorkspaceRole.MEMBER);
                workspaceMapper.updateInviteStatus(inviteId, InviteStatus.ACCEPTED);

                // 사용자는 JPA 엔티티로 들고 있어서 JPA로 바꿔요(6-3단계와 같은 이유)
                Workspace workspaceRef = workspaceRepository.getReferenceById(invite.getWorkspaceId());
                user.updateLastWorkspace(workspaceRef);

                // 받은 초대 알림은 읽음 처리하고, 초대한 사람에게 수락했다고 알려요.
                notificationService.markReadByRef(user, NotificationType.WORKSPACE_INVITE, inviteId);
                notificationService.send(findInviter(invite), user, workspaceRef, NotificationType.INVITE_ACCEPTED,
                        user.getNickname() + "님이 '" + findWorkspace(invite.getWorkspaceId()).getName()
                                + "' 초대를 수락했어요.",
                        inviteId, null);
        }

        // 워크스페이스 초대 거절
        @Override
        public void declineInvite(Long inviteId, String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                InviteVO invite = findMyPendingInvite(inviteId, user);

                workspaceMapper.updateInviteStatus(inviteId, InviteStatus.DECLINED);

                notificationService.markReadByRef(user, NotificationType.WORKSPACE_INVITE, inviteId);
                notificationService.send(findInviter(invite), user,
                        workspaceRepository.getReferenceById(invite.getWorkspaceId()), NotificationType.INVITE_DECLINED,
                        user.getNickname() + "님이 '" + findWorkspace(invite.getWorkspaceId()).getName()
                                + "' 초대를 거절했어요.",
                        inviteId, null);
        }

        // 나에게 온, 아직 답하지 않은 초대 (아니면 예외)
        private InviteVO findMyPendingInvite(Long inviteId, User user) {

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

        // 초대한 사람 (알림을 받을 사람이라 엔티티로 읽어요)
        private User findInviter(InviteVO invite) {
                return userRepository.findById(invite.getInviterId())
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));
        }

        // 워크스페이스 멤버 목록 조회
        @Transactional(readOnly = true)
        @Override
        public List<WorkspaceMemberResponse> getMembers(Long workspaceId, String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                findWorkspace(workspaceId);
                findMemberRole(workspaceId, user);

                // 접속 중인지는 DB가 아니라 서버 메모리(PresenceService)에 있어서 자바에서 붙여요
                return workspaceMapper.selectWorkspaceMemberList(workspaceId).stream()
                        .map(member -> WorkspaceMemberResponse.from(member, presenceService.isOnline(member.getUserId())))
                        .toList();
        }

        // 멤버 프로필 카드 조회 (같은 워크스페이스 멤버끼리만 볼 수 있고, 이 워크스페이스 안의 정보만 보여줘요)
        @Transactional(readOnly = true)
        @Override
        public MemberProfileResponse getMemberProfile(Long workspaceId, Long userId, String email) {

                User viewer = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                findWorkspace(workspaceId);
                findMemberRole(workspaceId, viewer);

                if (!userRepository.existsById(userId)) {
                        throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
                }

                WorkspaceMemberVO member = workspaceMapper.selectWorkspaceMember(workspaceId, userId);

                if (member == null) {
                        throw new FlowSpaceException(ErrorCode.MEMBER_NOT_FOUND);
                }

                List<MemberProfileResponse.TaskItem> tasks = workspaceMapper
                        .selectMemberOpenTaskList(workspaceId, userId, PROFILE_TASK_LIMIT).stream()
                        .map(MemberProfileResponse.TaskItem::from).toList();

                // 진행 중·완료 수를 따로 세던 것을 프로시저 한 번으로 세요
                MemberTaskCountVO count = workspaceMapper.selectMemberTaskCount(workspaceId, userId);

                return new MemberProfileResponse(member.getUserId(), member.getNickname(), member.getEmail(),
                        member.getBio(), member.getProfileImageUrl(), member.getRole(), member.getJoinedAt(),
                        presenceService.isOnline(userId), member.getLastActiveAt(), count.getOpenTaskCount(),
                        count.getDoneTaskCount(), tasks);
        }

        // 워크스페이스 소유권 이전
        @Override
        public void transferOwnership(Long workspaceId, WorkspaceOwnerTransferRequest request, String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                WorkspaceVO workspace = findWorkspace(workspaceId);

                if (findMemberRole(workspaceId, user) != WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                User targetUser = userRepository.findById(request.userId())
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                // 넘겨받을 사람도 이 워크스페이스 멤버여야 해요
                if (workspaceMapper.selectMemberRole(workspaceId, targetUser.getUserId()) == null) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                // 세 줄 모두 테이블 하나씩이고 값은 자바가 알고 있어서 XML SQL로 해요(같은 트랜잭션이라 함께 반영·취소돼요)
                workspaceMapper.updateMemberRole(workspaceId, user.getUserId(), WorkspaceRole.MEMBER);
                workspaceMapper.updateMemberRole(workspaceId, targetUser.getUserId(), WorkspaceRole.OWNER);
                workspaceMapper.updateWorkspaceOwner(workspaceId, targetUser.getUserId());

                notificationService.send(targetUser, user, workspaceRepository.getReferenceById(workspaceId),
                        NotificationType.OWNERSHIP_TRANSFERRED,
                        user.getNickname() + "님이 '" + workspace.getName() + "'의 소유권을 넘겼어요. 이제 내가 소유자예요.", null,
                        null);
        }

        // 워크스페이스 삭제
        @Override
        public void deleteWorkspace(Long workspaceId, String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                findWorkspace(workspaceId);

                if (findMemberRole(workspaceId, user) != WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                // 내가 속한 워크스페이스가 이것 하나뿐이면 삭제할 수 없어요(앱을 쓸 곳이 없어져요).
                if (workspaceMapper.selectMyWorkspaceCount(user.getUserId()) <= 1) {
                        throw new FlowSpaceException(ErrorCode.LAST_WORKSPACE);
                }

                // 작업을 먼저 지우고 워크스페이스를 지우는 일을 프로시저 한 번으로 해요
                workspaceMapper.deleteWorkspace(workspaceId);
        }

        // 워크스페이스 멤버 추방 (관리자는 일반 멤버만, 소유자는 관리자·멤버를 내보낼 수 있어요)
        @Override
        public void removeMember(Long workspaceId, Long userId, String email) {

                User loginUser = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                WorkspaceVO workspace = findWorkspace(workspaceId);

                WorkspaceRole actorRole = findMemberRole(workspaceId, loginUser);

                if (!actorRole.isAtLeast(WorkspaceRole.ADMIN)) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                User targetUser = userRepository.findById(userId)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                WorkspaceRole targetRole = workspaceMapper.selectMemberRole(workspaceId, userId);

                if (targetRole == null) {
                        throw new FlowSpaceException(ErrorCode.MEMBER_NOT_FOUND);
                }

                if (targetRole == WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.OWNER_CANNOT_REMOVE);
                }

                // 관리자는 다른 관리자를 내보낼 수 없어요. (내보내기 전에 소유자가 역할을 바꿔야 해요.)
                if (actorRole == WorkspaceRole.ADMIN && targetRole != WorkspaceRole.MEMBER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                workspaceMapper.deleteWorkspaceMember(workspaceId, userId);

                notificationService.send(targetUser, loginUser, workspaceRepository.getReferenceById(workspaceId),
                        NotificationType.MEMBER_REMOVED, "'" + workspace.getName() + "' 워크스페이스에서 내보내졌어요.", null,
                        null);
        }

        // 멤버 역할 변경 (소유자만 가능, ADMIN ↔ MEMBER)
        @Override
        public WorkspaceMemberResponse changeMemberRole(Long workspaceId, Long userId,
                WorkspaceRoleChangeRequest request, String email) {

                User loginUser = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                findWorkspace(workspaceId);

                if (findMemberRole(workspaceId, loginUser) != WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                // 소유자 자리는 소유권 이전으로만 바뀌어요.
                if (request.role() == WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.INVALID_ROLE_CHANGE);
                }

                if (!userRepository.existsById(userId)) {
                        throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
                }

                // 응답에 이름·사진이 필요해서 역할만이 아니라 멤버 정보를 통째로 읽어요
                WorkspaceMemberVO member = workspaceMapper.selectWorkspaceMember(workspaceId, userId);

                if (member == null) {
                        throw new FlowSpaceException(ErrorCode.MEMBER_NOT_FOUND);
                }

                if (member.getRole() == WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.INVALID_ROLE_CHANGE);
                }

                workspaceMapper.updateMemberRole(workspaceId, userId, request.role());

                // DB가 따로 채우는 값이 없어서, 바꾼 역할을 VO에 넣어 바로 응답해요
                member.setRole(request.role());

                return WorkspaceMemberResponse.from(member, presenceService.isOnline(userId));
        }

        // 워크스페이스 나가기
        @Override
        public void leaveWorkspace(Long workspaceId, String email) {

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                WorkspaceVO workspace = findWorkspace(workspaceId);

                if (findMemberRole(workspaceId, user) == WorkspaceRole.OWNER) {
                        throw new FlowSpaceException(ErrorCode.OWNER_CANNOT_LEAVE);
                }

                // 내가 속한 워크스페이스가 이것 하나뿐이면 나갈 수 없어요.
                if (workspaceMapper.selectMyWorkspaceCount(user.getUserId()) <= 1) {
                        throw new FlowSpaceException(ErrorCode.LAST_WORKSPACE);
                }

                workspaceMapper.deleteWorkspaceMember(workspaceId, user.getUserId());

                // 소유자에게 멤버가 나갔다고 알려요. (알림 기능이 엔티티를 받아서 소유자를 엔티티로 읽어요)
                User owner = userRepository.findById(workspace.getOwnerId())
                        .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

                notificationService.send(owner, user, workspaceRepository.getReferenceById(workspaceId),
                        NotificationType.MEMBER_LEFT,
                        user.getNickname() + "님이 '" + workspace.getName() + "' 워크스페이스를 나갔어요.", null, null);

                // 사용자는 JPA 엔티티로 들고 있어서 JPA로 바꿔요(6-3단계와 같은 이유)
                if (user.getLastWorkspace() != null && user.getLastWorkspace().getWorkspaceId().equals(workspaceId)) {
                        user.updateLastWorkspace(null);
                }
        }

        // 워크스페이스 한 건 (없으면 예외)
        private WorkspaceVO findWorkspace(Long workspaceId) {

                // MyBatis는 결과가 없으면 null을 돌려줘서 직접 확인해요
                WorkspaceVO workspace = workspaceMapper.selectWorkspaceById(workspaceId);

                if (workspace == null) {
                        throw new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND);
                }

                return workspace;
        }

        // 로그인한 사람의 그 워크스페이스 역할 (멤버가 아니면 예외)
        private WorkspaceRole findMemberRole(Long workspaceId, User user) {

                WorkspaceRole role = workspaceMapper.selectMemberRole(workspaceId, user.getUserId());

                if (role == null) {
                        throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
                }

                return role;
        }
}
