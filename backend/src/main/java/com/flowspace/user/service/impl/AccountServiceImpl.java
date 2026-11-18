package com.flowspace.user.service.impl;

import com.flowspace.service.FileService;
import com.flowspace.notification.service.NotificationService;
import com.flowspace.user.service.AccountService;
import com.flowspace.user.service.UserVO;
import com.flowspace.user.service.WithdrawWorkspaceVO;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.user.dto.response.WithdrawCheckResponse;
import com.flowspace.user.dto.response.WithdrawCheckResponse.WorkspaceBrief;
import com.flowspace.user.dto.request.WithdrawRequest;
import com.flowspace.entity.User;
import com.flowspace.entity.enums.NotificationType;
import com.flowspace.entity.enums.Provider;
import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.websocket.NotificationWebSocketHandler;

import lombok.RequiredArgsConstructor;

// 계정 관리 중에서 회원 탈퇴를 맡아요.
// 작업·댓글·활동 기록은 다른 멤버들에게도 필요해서 사용자 행을 지우지 않고 "탈퇴한 사용자"로 익명 처리해요(프로시저 sp_withdraw_user).
@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final NotificationService notificationService;
    private final NotificationWebSocketHandler socketHandler;

    // 탈퇴하면 내 워크스페이스가 어떻게 되는지 미리 알려줘요.
    @Transactional(readOnly = true)
    @Override
    public WithdrawCheckResponse checkWithdraw(String email) {

        UserVO user = findUser(email);

        return classify(user.getUserId()).toResponse();
    }

    // 회원 탈퇴
    @Override
    public void withdraw(WithdrawRequest request, String email) {

        UserVO user = findUser(email);

        confirmIdentity(user, request);

        Classified classified = classify(user.getUserId());

        // 다른 멤버가 있는 워크스페이스의 소유자는 먼저 소유권을 넘기거나 멤버를 내보내야 해요.
        if (!classified.blocking.isEmpty()) {
            throw new FlowSpaceException(ErrorCode.OWNED_WORKSPACE_HAS_MEMBERS);
        }

        // 프로필 사진 파일 삭제 (파일을 못 지워도 탈퇴는 진행해요)
        if (user.getProfileFileId() != null) {
            try {
                fileService.delete(user.getProfileFileId());
            } catch (FlowSpaceException ignored) {
            }
        }

        // 내가 멤버로 있던 다른 사람의 워크스페이스 소유자에게 알려요. (멤버에서 빠지는 일은 프로시저가 해요)
        for (WithdrawWorkspaceVO workspace : classified.leaving) {
            notificationService.send(workspace.getOwnerId(), user.getUserId(), workspace.getWorkspaceId(),
                NotificationType.MEMBER_LEFT,
                user.getNickname() + "님이 탈퇴하면서 '" + workspace.getName() + "' 워크스페이스를 나갔어요.", null, null);
        }

        // 나가기, 내 워크스페이스 삭제, 연결 기록 삭제, 개인정보 지우기를 프로시저 한 번으로 해요
        userMapper.updateUserWithdraw(user.getUserId(), User.WITHDRAWN_NICKNAME);

        Long userId = user.getUserId();

        // 커밋된 뒤에 열려 있는 모든 접속(탭)을 끊어요.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.disconnectUser(userId);
                }
            });
        } else {
            socketHandler.disconnectUser(userId);
        }
    }

    // 본인 확인: 이메일 가입 계정은 비밀번호, 소셜 계정은 계정 이메일을 그대로 입력하게 해요.
    private void confirmIdentity(UserVO user, WithdrawRequest request) {

        if (user.getProvider() == Provider.LOCAL && user.getPassword() != null) {
            if (request.password() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new FlowSpaceException(ErrorCode.INVALID_CURRENT_PASSWORD);
            }
            return;
        }

        if (request.confirmEmail() == null || !request.confirmEmail().trim().equalsIgnoreCase(user.getEmail())) {
            throw new FlowSpaceException(ErrorCode.WITHDRAW_CONFIRM_MISMATCH);
        }
    }

    // 탈퇴하지 않은 사용자 (없거나 이미 탈퇴했으면 예외)
    private UserVO findUser(String email) {

        UserVO user = userMapper.selectUserByEmail(email);

        if (user == null || user.getDeletedAt() != null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }

    // 내 워크스페이스를 탈퇴 때의 처리 방식별로 나눠요.
    private Classified classify(Long userId) {

        Classified result = new Classified();

        for (WithdrawWorkspaceVO workspace : userMapper.selectWithdrawCheckList(userId)) {

            if (workspace.getRole() == WorkspaceRole.OWNER) {
                if (workspace.getMemberCount() > 1) {
                    result.blocking.add(workspace);
                } else {
                    result.deleting.add(workspace);
                }
            } else {
                result.leaving.add(workspace);
            }
        }

        return result;
    }

    private static class Classified {

        final List<WithdrawWorkspaceVO> blocking = new ArrayList<>();
        final List<WithdrawWorkspaceVO> deleting = new ArrayList<>();
        final List<WithdrawWorkspaceVO> leaving = new ArrayList<>();

        WithdrawCheckResponse toResponse() {
            return new WithdrawCheckResponse(blocking.isEmpty(), brief(blocking), brief(deleting), brief(leaving));
        }

        private static List<WorkspaceBrief> brief(List<WithdrawWorkspaceVO> list) {
            return list.stream()
                .map(workspace -> new WorkspaceBrief(workspace.getWorkspaceId(), workspace.getName(),
                    workspace.getMemberCount()))
                .toList();
        }
    }
}
