package com.flowspace.user.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 메일 링크 토큰 (email_verification_tokens·password_reset_tokens 공용)
// 원본 토큰 대신 SHA-256 해시만 저장
@Getter
@Setter
@NoArgsConstructor
public class MailTokenVO {

    private Long userId;

    private String tokenHash;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}
