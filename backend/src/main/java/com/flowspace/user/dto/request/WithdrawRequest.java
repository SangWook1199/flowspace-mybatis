package com.flowspace.user.dto.request;

import jakarta.validation.constraints.Size;

// @formatter:off

// 회원 탈퇴 요청 DTO
// 이메일 가입 계정은 password, 소셜 계정은 confirmEmail(계정 이메일)로 본인 확인
public record WithdrawRequest(

    @Size(max = 100, message = "비밀번호가 너무 깁니다.")
    String password,

    @Size(max = 255, message = "이메일이 너무 깁니다.")
    String confirmEmail

) {}

// @formatter:on
