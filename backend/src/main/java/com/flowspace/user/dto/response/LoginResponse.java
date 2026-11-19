package com.flowspace.user.dto.response;

import com.flowspace.user.service.UserVO;

// @formatter:off
public record LoginResponse(

    UserResponse user,

    String accessToken,
    String refreshToken,

    Long workspaceId

) {

    // UserVO 기반 응답 생성
    public static LoginResponse from(
        UserVO user,
        String accessToken,
        String refreshToken,
        Long workspaceId
    ) {
        return new LoginResponse(
            UserResponse.from(user),
            accessToken,
            refreshToken,
            workspaceId
        );
    }
}