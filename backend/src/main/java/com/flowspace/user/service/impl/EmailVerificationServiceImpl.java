package com.flowspace.user.service.impl;

import com.flowspace.service.MailService;
import com.flowspace.user.service.EmailVerificationService;
import com.flowspace.user.service.MailTokenVO;
import com.flowspace.user.service.UserVO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.user.dto.request.EmailResendRequest;
import com.flowspace.user.dto.request.EmailVerifyRequest;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 회원가입 이메일 인증 (인증 링크 발송, 링크 확인 후 로그인 가능)
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationServiceImpl implements EmailVerificationService {

    // 링크가 살아 있는 시간(시간)
    private static final int VALID_HOURS = 24;
    // 인증 메일 재발송 대기 시간 (화면의 "다시 보내기" 대기 시간과 동일)
    private static final Duration RESEND_INTERVAL = Duration.ofSeconds(30);

    private static final SecureRandom RANDOM = new SecureRandom();

    // 사용자·토큰 SQL
    private final UserMapper userMapper;
    private final MailService mailService;

    // 프런트 주소(메일 링크의 앞부분). 예: https://myflowspace.cloud
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // 인증 메일 발송 (가입 직후, 재가입, 다시 보내기 공용)
    // 직전에 보냈으면 생략 (메일 폭탄 방지)
    @Override
    public void sendVerification(Long userId, String email) {

        LocalDateTime lastSentAt = userMapper.selectLastEmailTokenCreatedAt(userId);

        if (lastSentAt != null && lastSentAt.plus(RESEND_INTERVAL).isAfter(LocalDateTime.now())) {
            log.info("인증 메일을 방금 보내서 건너뛰었어요. email={}", email);
            return;
        }

        userMapper.deleteEmailTokens(userId);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        userMapper.insertEmailToken(userId, hash(rawToken), LocalDateTime.now().plusHours(VALID_HOURS));

        String link = frontendUrl.replaceAll("/+$", "") + "/verify-email?token=" + rawToken;

        mailService.sendEmailVerification(email, link, VALID_HOURS);
    }

    // 인증 메일 다시 보내기
    // 가입 여부 노출 방지 (없는 이메일·인증 완료 계정도 성공 응답, 메일은 보내지 않음)
    @Override
    public void resend(EmailResendRequest request) {

        UserVO user = userMapper.selectUserByEmail(request.email().trim());

        if (user == null || user.getDeletedAt() != null || Boolean.TRUE.equals(user.getEmailVerified())) {
            log.info("인증 메일을 보내지 않았어요(가입된 이메일이 아니거나 이미 인증한 계정). email={}", request.email());
            return;
        }

        sendVerification(user.getUserId(), user.getEmail());
    }

    // 이메일 인증 (링크가 맞고 만료 전이면 인증 완료, 링크는 1회용으로 삭제)
    @Override
    public void verify(EmailVerifyRequest request) {

        MailTokenVO saved = userMapper.selectEmailTokenByHash(hash(request.token().trim()));

        if (saved == null) {
            throw new FlowSpaceException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }

        UserVO user = userMapper.selectUserById(saved.getUserId());

        if (saved.getExpiresAt().isBefore(LocalDateTime.now()) || user == null || user.getDeletedAt() != null) {
            throw new FlowSpaceException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }

        userMapper.updateUserEmailVerified(user.getUserId());

        userMapper.deleteEmailTokens(user.getUserId());
    }

    private static String hash(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
