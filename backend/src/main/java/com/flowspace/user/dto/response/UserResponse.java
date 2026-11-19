package com.flowspace.user.dto.response;

import com.flowspace.common.enums.Provider;
import com.flowspace.user.service.UserVO;

// @formatter:off
public record UserResponse(
        Long userId,
        String email,
        String nickname,
        Provider provider,
        String bio,
        String profileImageUrl) {
    // UserVO 기반 응답 생성 (비밀번호 제외)
    public static UserResponse from(UserVO user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider(),
                user.getBio(),
                user.getProfileImageUrl());
    }
}