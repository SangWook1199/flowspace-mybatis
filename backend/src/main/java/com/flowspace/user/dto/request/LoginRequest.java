package com.flowspace.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// @formatter:off
public record LoginRequest(

    @NotBlank @Email
    String email,

    @NotBlank
    String password,

    // 로그인 상태 유지 (null이면 유지)
    Boolean rememberMe

) {

    public boolean remember() {
        return rememberMe == null || rememberMe;
    }
}
// @formatter:on
