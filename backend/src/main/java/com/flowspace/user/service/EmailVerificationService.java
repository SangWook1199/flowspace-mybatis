package com.flowspace.user.service;

import com.flowspace.user.dto.request.EmailResendRequest;
import com.flowspace.user.dto.request.EmailVerifyRequest;

// 이메일 인증 서비스
public interface EmailVerificationService {

    // 직전에 보냈으면 생략 (메일 폭탄 방지)
    void sendVerification(Long userId, String email);

    // 가입 여부 노출 방지 (없는 이메일·인증 완료 계정도 성공 응답, 메일은 보내지 않음)
    void resend(EmailResendRequest request);

    // 이메일 인증 (링크가 맞고 만료 전이면 인증 완료, 링크는 1회용으로 삭제)
    void verify(EmailVerifyRequest request);
}
