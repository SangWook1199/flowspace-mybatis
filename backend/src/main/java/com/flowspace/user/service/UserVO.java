package com.flowspace.user.service;

import java.time.LocalDateTime;

import com.flowspace.entity.enums.Provider;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사용자 한 명을 담는 객체예요(users 테이블 + 프로필 이미지 주소).
// 프로필 이미지와 마지막으로 본 워크스페이스는 객체가 아니라 번호로 가져요.
@Getter
@Setter
@NoArgsConstructor
public class UserVO {

    private Long userId;

    private String email;

    // 암호화된 비밀번호 (구글·MS 계정은 null)
    private String password;

    private String nickname;

    // LOCAL, GOOGLE, MICROSOFT, APPLE
    private Provider provider;

    private String providerId;

    private Boolean emailVerified;

    private LocalDateTime lastActiveAt;

    private String bio;

    // 탈퇴한 시각 (탈퇴하지 않았으면 null)
    private LocalDateTime deletedAt;

    private Long profileFileId;

    // files 테이블에서 조인해 온 값이에요
    private String profileImageUrl;

    private Long lastWorkspaceId;

    private LocalDateTime createdAt;
}
