package com.flowspace.user.service;

import java.util.Optional;

import com.flowspace.user.dto.response.UserLookupResponse;

// 사용자 서비스
public interface UserService {

    // workspaceId가 있으면 해당 워크스페이스의 멤버·초대 여부 포함 (워크스페이스 멤버만 사용 가능)
    Optional<UserLookupResponse> lookupByEmail(String email, Long workspaceId, String requesterEmail);
}
