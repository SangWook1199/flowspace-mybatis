package com.flowspace.user.dto.request;

import com.flowspace.common.enums.Provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 소셜 계정 연결 요청 (같은 이메일 계정의 비밀번호로 본인 확인)
// provider: GOOGLE | MICROSOFT, accessToken: Microsoft 프로필 사진용 (선택)
// @formatter:off
public record SocialLinkRequest(

    @NotNull
    Provider provider,

    @NotBlank
    String idToken,

    String accessToken,

    @NotBlank
    String password

) {}
// @formatter:on
