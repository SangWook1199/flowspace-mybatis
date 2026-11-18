package com.flowspace.user.service;

import java.util.Optional;

import com.flowspace.user.dto.response.UserLookupResponse;

// UserServiceImpl가 실제 동작을 맡아요. Controller와 다른 서비스는 이 인터페이스만 알아요.
public interface UserService {

    // workspaceId를 주면 그 워크스페이스 기준으로 이미 멤버인지·이미 초대했는지도 알려줘요(워크스페이스 멤버만 쓸 수 있어요).
    Optional<UserLookupResponse> lookupByEmail(String email, Long workspaceId, String requesterEmail);
}
