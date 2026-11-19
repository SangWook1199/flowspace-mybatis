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

    // 이메일이 정확히 일치하는 사용자 한 명 조회 (부분 일치 검색 방지)
    // workspaceId가 있으면 해당 워크스페이스의 멤버·초대 여부 포함 (워크스페이스 멤버만 사용 가능)
    @Override
    public Optional<UserLookupResponse> lookupByEmail(String email, Long workspaceId, String requesterEmail) {

        UserVO requester = userMapper.selectUserByEmail(requesterEmail);

        if (requester == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        if (workspaceId != null) {
            workspaceService.getWorkspace(workspaceId, requesterEmail);
        }

        String target = email == null ? "" : email.trim();

        if (target.isEmpty()) {
            return Optional.empty();
        }

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
