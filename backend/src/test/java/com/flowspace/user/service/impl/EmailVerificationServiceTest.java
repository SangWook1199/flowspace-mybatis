package com.flowspace.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.flowspace.common.mail.MailService;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.user.dto.request.EmailResendRequest;
import com.flowspace.user.dto.request.EmailVerifyRequest;
import com.flowspace.user.service.MailTokenVO;
import com.flowspace.user.service.UserVO;

// 이메일 인증: 해시 저장, 재발송 제한, 가입 여부 비노출, 토큰 확인 규칙
// Mapper 반환 값을 지정하고 호출된 SQL 확인
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private MailService mailService;

    @InjectMocks
    private EmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173/");
    }

    private static String sha256(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    }

    // 테스트 사용자 (비밀번호·인증 여부만 다름)
    private static UserVO user(Long id, String email, String password, boolean verified) {
        UserVO user = new UserVO();
        user.setUserId(id);
        user.setEmail(email);
        user.setPassword(password);
        user.setEmailVerified(verified);
        return user;
    }

    // 저장된 토큰 한 개
    private static MailTokenVO token(String hash, LocalDateTime expiresAt) {
        MailTokenVO token = new MailTokenVO();
        token.setUserId(1L);
        token.setTokenHash(hash);
        token.setExpiresAt(expiresAt);
        return token;
    }

    @Test
    @DisplayName("메일 링크의 원본 토큰은 저장하지 않고 해시만 저장한다")
    void storesOnlyTheHash() throws Exception {
        when(userMapper.selectLastEmailTokenCreatedAt(1L)).thenReturn(null);

        service.sendVerification(1L, "a@a.com");

        ArgumentCaptor<String> savedHash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expiresAt = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);

        verify(userMapper).deleteEmailTokens(1L);
        verify(userMapper).insertEmailToken(eq(1L), savedHash.capture(), expiresAt.capture());
        verify(mailService).sendEmailVerification(eq("a@a.com"), link.capture(), anyInt());

        String prefix = "http://localhost:5173/verify-email?token=";
        assertTrue(link.getValue().startsWith(prefix));

        String rawToken = link.getValue().substring(prefix.length());

        assertEquals(sha256(rawToken), savedHash.getValue());
        assertFalse(savedHash.getValue().contains(rawToken));
        assertTrue(expiresAt.getValue().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("방금 보냈으면 다시 보내지 않는다")
    void tooSoonIsSkipped() {
        when(userMapper.selectLastEmailTokenCreatedAt(1L)).thenReturn(LocalDateTime.now());

        service.sendVerification(1L, "a@a.com");

        verifyNoInteractions(mailService);
        verify(userMapper, never()).insertEmailToken(any(), any(), any());
    }

    @Test
    @DisplayName("가입하지 않은 이메일이면 다시 보내기도 아무것도 보내지 않는다")
    void resendToUnknownEmailSendsNothing() {
        when(userMapper.selectUserByEmail("none@a.com")).thenReturn(null);

        service.resend(new EmailResendRequest("none@a.com"));

        verify(userMapper, never()).insertEmailToken(any(), any(), any());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("이미 인증한 계정에는 다시 보내지 않는다")
    void resendToVerifiedAccountSendsNothing() {
        when(userMapper.selectUserByEmail("v@a.com")).thenReturn(user(2L, "v@a.com", "PW", true));

        service.resend(new EmailResendRequest("v@a.com"));

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("없는 토큰은 거절한다")
    void unknownTokenIsRejected() {
        when(userMapper.selectEmailTokenByHash(anyString())).thenReturn(null);

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> service.verify(new EmailVerifyRequest("nope")));

        assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, e.getErrorCode());
    }

    @Test
    @DisplayName("만료된 토큰은 거절하고 인증하지 않는다")
    void expiredTokenIsRejected() throws Exception {
        when(userMapper.selectEmailTokenByHash(sha256("raw"))).thenReturn(token(sha256("raw"), LocalDateTime.now().minusMinutes(1)));
        when(userMapper.selectUserById(1L)).thenReturn(user(1L, "a@a.com", "PW", false));

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> service.verify(new EmailVerifyRequest("raw")));

        assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, e.getErrorCode());
        verify(userMapper, never()).updateUserEmailVerified(any());
    }

    @Test
    @DisplayName("맞는 토큰이면 인증을 마치고 토큰을 지운다")
    void validTokenVerifiesTheUser() throws Exception {
        when(userMapper.selectEmailTokenByHash(sha256("raw"))).thenReturn(token(sha256("raw"), LocalDateTime.now().plusHours(1)));
        when(userMapper.selectUserById(1L)).thenReturn(user(1L, "a@a.com", "PW", false));

        service.verify(new EmailVerifyRequest("raw"));

        verify(userMapper).updateUserEmailVerified(1L);
        verify(userMapper).deleteEmailTokens(1L);
    }
}
