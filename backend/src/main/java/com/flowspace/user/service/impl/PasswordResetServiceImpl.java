package com.flowspace.user.service.impl;

import com.flowspace.service.MailService;
import com.flowspace.user.service.MailTokenVO;
import com.flowspace.user.service.PasswordResetService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.user.dto.request.PasswordForgotRequest;
import com.flowspace.user.dto.request.PasswordResetRequest;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 비밀번호 찾기 (재설정 링크 발송, 링크 토큰으로 새 비밀번호 설정)
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    // 링크가 살아 있는 시간(분)
    private static final int VALID_MINUTES = 30;
    // 인증 메일 재발송 대기 시간 (화면의 "다시 보내기" 대기 시간과 동일)
    private static final Duration RESEND_INTERVAL = Duration.ofSeconds(10);

    private static final SecureRandom RANDOM = new SecureRandom();

    // 사용자·토큰 SQL
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    // 프런트 주소(메일 링크의 앞부분). 예: http://localhost:5173
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // 재설정 메일 발송
    // 가입 여부 노출 방지 (없는 이메일도 성공 응답)
    // 비밀번호가 없는 계정(소셜 전용)은 발송하지 않음
    @Override
    public void requestReset(PasswordForgotRequest request) {

        UserVO user = userMapper.selectUserByEmail(request.email().trim());

        if (user == null || user.getDeletedAt() != null || user.getPassword() == null) {
            log.info("비밀번호 재설정 메일을 보내지 않았어요(가입된 이메일이 아니거나 비밀번호가 없는 소셜 전용 계정). email={}", request.email());
            return;
        }

        LocalDateTime lastSentAt = userMapper.selectLastPasswordTokenCreatedAt(user.getUserId());

        if (lastSentAt != null && lastSentAt.plus(RESEND_INTERVAL).isAfter(LocalDateTime.now())) {
            log.info("비밀번호 재설정 메일을 방금 보내서 건너뛰었어요. email={}", user.getEmail());
            return;
        }

        userMapper.deletePasswordTokens(user.getUserId());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        userMapper.insertPasswordToken(user.getUserId(), hash(rawToken), LocalDateTime.now().plusMinutes(VALID_MINUTES));

        String link = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + rawToken;

        mailService.sendPasswordReset(user.getEmail(), link, VALID_MINUTES);
    }

    // 새 비밀번호 설정 (링크가 맞고 만료 전이면 변경, 링크는 1회용으로 삭제)
    // 모든 기기 로그인 해제 (비밀번호 분실 상황 고려)
    @Override
    public void reset(PasswordResetRequest request) {

        MailTokenVO saved = userMapper.selectPasswordTokenByHash(hash(request.token().trim()));

        if (saved == null) {
            throw new FlowSpaceException(ErrorCode.INVALID_RESET_TOKEN);
        }

        UserVO user = userMapper.selectUserById(saved.getUserId());

        if (saved.getExpiresAt().isBefore(LocalDateTime.now()) || user == null || user.getDeletedAt() != null) {
            userMapper.deletePasswordTokenByHash(saved.getTokenHash());
            throw new FlowSpaceException(ErrorCode.INVALID_RESET_TOKEN);
        }

        if (user.getPassword() != null && passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new FlowSpaceException(ErrorCode.SAME_PASSWORD);
        }

        userMapper.updateUserPassword(user.getUserId(), passwordEncoder.encode(request.newPassword()));
        userMapper.updateUserEmailVerified(user.getUserId());

        userMapper.deletePasswordTokens(user.getUserId());
        userMapper.deleteRefreshToken(user.getUserId());
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
