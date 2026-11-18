package com.flowspace.user.service;

import com.flowspace.user.dto.request.EmailResendRequest;
import com.flowspace.user.dto.request.EmailVerifyRequest;

// EmailVerificationServiceImpl가 실제 동작을 맡아요. Controller와 다른 서비스는 이 인터페이스만 알아요.
public interface EmailVerificationService {

    // 방금 보냈으면 건너뛰어요 — 남의 이메일로 메일이 쏟아지게 하는 데 쓰이지 않게요.
    void sendVerification(Long userId, String email);

    // 가입 여부를 알아내는 데 쓰이지 않게, 없는 이메일이나 이미 인증한 계정이어도 똑같이 성공으로 답해요(아무것도 보내지 않을 뿐이에요).
    void resend(EmailResendRequest request);

    // 인증하기: 링크가 맞고 만료 전이면 인증을 마치고, 링크는 지워요(한 번만 쓸 수 있어요).
    void verify(EmailVerifyRequest request);
}
