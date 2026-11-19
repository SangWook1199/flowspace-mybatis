package com.flowspace.user.service.impl;

import com.flowspace.file.service.FileService;
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
import com.flowspace.common.enums.NotificationType;
import com.flowspace.common.enums.Provider;
import com.flowspace.common.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.websocket.NotificationWebSocketHandler;

import lombok.RequiredArgsConstructor;

// 회원 탈퇴
// 작업·댓글·활동 기록 보존을 위해 사용자 행은 삭제하지 않고 익명 처리 (sp_withdraw_user)
@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    // 탈퇴 후 표시 이름
    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final NotificationService notificationService;
    private final NotificationWebSocketHandler socketHandler;

    // 탈퇴 시 워크스페이스 처리 방식 미리 확인
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

        if (!classified.blocking.isEmpty()) {
            throw new FlowSpaceException(ErrorCode.OWNED_WORKSPACE_HAS_MEMBERS);
        }

        if (user.getProfileFileId() != null) {
            try {
                fileService.delete(user.getProfileFileId());
            } catch (FlowSpaceException ignored) {
            }
        }

        for (WithdrawWorkspaceVO workspace : classified.leaving) {
            notificationService.send(workspace.getOwnerId(), user.getUserId(), workspace.getWorkspaceId(),
                NotificationType.MEMBER_LEFT,
                user.getNickname() + "님이 탈퇴하면서 '" + workspace.getName() + "' 워크스페이스를 나갔어요.", null, null);
        }

        userMapper.updateUserWithdraw(user.getUserId(), WITHDRAWN_NICKNAME);

        Long userId = user.getUserId();

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

    // 본인 확인: 이메일 가입 계정은 비밀번호, 소셜 계정은 계정 이메일 입력
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

    // 소유 워크스페이스를 탈퇴 처리 방식별로 분류
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
