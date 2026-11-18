package com.flowspace.user.service.impl;

import com.flowspace.user.service.UserLookupVO;
import com.flowspace.user.service.UserService;
import com.flowspace.user.service.UserVO;
import com.flowspace.workspace.service.WorkspaceService;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.user.dto.response.UserLookupResponse;
import com.flowspace.user.dto.response.UserLookupResponse.Status;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final WorkspaceService workspaceService;

    // 이메일이 정확히 같은 사용자를 찾아요(부분 일치로 사용자를 훑어볼 수 없게 한 명만, 정확히 일치할 때만).
    // workspaceId를 주면 그 워크스페이스 기준으로 이미 멤버인지·이미 초대했는지도 알려줘요(워크스페이스 멤버만 쓸 수 있어요).
    @Override
    public Optional<UserLookupResponse> lookupByEmail(String email, Long workspaceId, String requesterEmail) {

        UserVO requester = userMapper.selectUserByEmail(requesterEmail);

        if (requester == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        // 워크스페이스가 있는지, 찾는 사람이 그 멤버인지는 워크스페이스 서비스의 규칙을 그대로 써요
        // (없으면 WORKSPACE_NOT_FOUND, 멤버가 아니면 ACCESS_DENIED)
        if (workspaceId != null) {
            workspaceService.getWorkspace(workspaceId, requesterEmail);
        }

        String target = email == null ? "" : email.trim();

        if (target.isEmpty()) {
            return Optional.empty();
        }

        // 사용자와 멤버·초대 여부를 프로시저 한 번으로 가져와요
        UserLookupVO found = userMapper.selectUserLookup(target, workspaceId);

        if (found == null) {
            return Optional.empty();
        }

        return Optional.of(UserLookupResponse.of(found, resolveStatus(found, requester, workspaceId)));
    }

    private Status resolveStatus(UserLookupVO found, UserVO requester, Long workspaceId) {

        if (found.getUserId().equals(requester.getUserId())) {
            return Status.SELF;
        }

        if (workspaceId == null) {
            return Status.AVAILABLE;
        }

        if (Boolean.TRUE.equals(found.getIsMember())) {
            return Status.MEMBER;
        }

        if (Boolean.TRUE.equals(found.getIsInvited())) {
            return Status.INVITED;
        }

        return Status.AVAILABLE;
    }
}
