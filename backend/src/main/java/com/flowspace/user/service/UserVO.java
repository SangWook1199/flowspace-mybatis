package com.flowspace.user.service;

import java.time.LocalDateTime;

import com.flowspace.common.enums.Provider;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사용자 (users 테이블 + 프로필 이미지 주소)
// 프로필 이미지·마지막 워크스페이스는 번호로 보관
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

    // files 조인 컬럼
    private String profileImageUrl;

    private Long lastWorkspaceId;

    private LocalDateTime createdAt;
}
