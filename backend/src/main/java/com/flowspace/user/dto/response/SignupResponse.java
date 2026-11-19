package com.flowspace.user.dto.response;

// 회원가입 결과 (이메일 인증 후 로그인 가능, 토큰 대신 인증 메일 주소 반환)
// @formatter:off
public record SignupResponse(

    String email

) {}
// @formatter:on
