package com.flowspace.user.dto.response;

import com.flowspace.entity.User;
import com.flowspace.entity.enums.Provider;
import com.flowspace.user.service.UserVO;

// @formatter:off
public record UserResponse(
        Long userId,
        String email,
        String nickname,
        Provider provider,
        String bio,
        String profileImageUrl) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider(),
                user.getBio(),
                user.getProfileFile() == null ? null : user.getProfileFile().getFileUrl());
    }

    // MyBatis 조회 결과(UserVO)로 응답을 만들어요 (비밀번호는 넣지 않아요)
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