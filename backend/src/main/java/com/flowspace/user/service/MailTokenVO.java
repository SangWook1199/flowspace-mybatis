package com.flowspace.user.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 메일 링크 토큰 한 건이에요. 이메일 인증(email_verification_tokens)과 비밀번호 재설정(password_reset_tokens)
// 테이블의 모양이 같아서 같이 써요. 원본 토큰은 저장하지 않고 SHA-256 해시만 저장해요.
@Getter
@Setter
@NoArgsConstructor
public class MailTokenVO {

    private Long userId;

    private String tokenHash;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}
