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
import com.flowspace.user.service.MailTokenVO;
import com.flowspace.user.service.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.flowspace.user.dto.request.PasswordForgotRequest;
import com.flowspace.user.dto.request.PasswordResetRequest;

// 비밀번호 재설정: 가입 여부 비노출, 해시 저장, 토큰·비밀번호 규칙, 로그인 해제
// Mapper 반환 값을 지정하고 호출된 SQL 확인
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @InjectMocks
    private PasswordResetServiceImpl service;

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
    @DisplayName("가입하지 않은 이메일이면 아무것도 보내지 않는다")
    void unknownEmailSendsNothing() {
        when(userMapper.selectUserByEmail("none@a.com")).thenReturn(null);

        service.requestReset(new PasswordForgotRequest("none@a.com"));

        verify(userMapper, never()).insertPasswordToken(any(), any(), any());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("비밀번호가 없는 소셜 전용 계정에는 보내지 않는다")
    void socialOnlyAccountSendsNothing() {
        when(userMapper.selectUserByEmail("s@a.com")).thenReturn(user(2L, "s@a.com", null, true));

        service.requestReset(new PasswordForgotRequest("s@a.com"));

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("방금 보냈으면 다시 보내지 않는다")
    void tooSoonIsSkipped() {
        when(userMapper.selectUserByEmail("a@a.com")).thenReturn(user(1L, "a@a.com", "OLD", true));
        when(userMapper.selectLastPasswordTokenCreatedAt(1L)).thenReturn(LocalDateTime.now());

        service.requestReset(new PasswordForgotRequest("a@a.com"));

        verifyNoInteractions(mailService);
        verify(userMapper, never()).insertPasswordToken(any(), any(), any());
    }

    @Test
    @DisplayName("메일 링크의 원본 토큰은 저장하지 않고 해시만 저장한다")
    void storesOnlyTheHash() throws Exception {
        when(userMapper.selectUserByEmail("a@a.com")).thenReturn(user(1L, "a@a.com", "OLD", true));
        when(userMapper.selectLastPasswordTokenCreatedAt(1L)).thenReturn(null);

        service.requestReset(new PasswordForgotRequest("a@a.com"));

        ArgumentCaptor<String> savedHash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expiresAt = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);

        verify(userMapper).deletePasswordTokens(1L);
        verify(userMapper).insertPasswordToken(eq(1L), savedHash.capture(), expiresAt.capture());
        verify(mailService).sendPasswordReset(eq("a@a.com"), link.capture(), anyInt());

        String prefix = "http://localhost:5173/reset-password?token=";
        assertTrue(link.getValue().startsWith(prefix));

        String rawToken = link.getValue().substring(prefix.length());

        assertEquals(64, savedHash.getValue().length());
        assertFalse(savedHash.getValue().contains(rawToken));
        assertEquals(sha256(rawToken), savedHash.getValue());
        assertTrue(expiresAt.getValue().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("없는 토큰은 거절한다")
    void unknownTokenIsRejected() {
        when(userMapper.selectPasswordTokenByHash(anyString())).thenReturn(null);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> service.reset(new PasswordResetRequest("nope", "newPassword1")));

        assertEquals(ErrorCode.INVALID_RESET_TOKEN, e.getErrorCode());
    }

    @Test
    @DisplayName("만료된 토큰은 지우고 거절한다")
    void expiredTokenIsRejectedAndDeleted() {
        when(userMapper.selectPasswordTokenByHash(anyString())).thenReturn(token("h", LocalDateTime.now().minusMinutes(1)));
        when(userMapper.selectUserById(1L)).thenReturn(user(1L, "a@a.com", "OLD", true));

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> service.reset(new PasswordResetRequest("raw", "newPassword1")));

        assertEquals(ErrorCode.INVALID_RESET_TOKEN, e.getErrorCode());
        verify(userMapper).deletePasswordTokenByHash("h");
        verify(userMapper, never()).deleteRefreshToken(any());
    }

    @Test
    @DisplayName("지금 비밀번호와 같으면 거절하고 링크는 남겨둔다")
    void samePasswordIsRejected() {
        when(userMapper.selectPasswordTokenByHash(anyString())).thenReturn(token("h", LocalDateTime.now().plusMinutes(10)));
        when(userMapper.selectUserById(1L)).thenReturn(user(1L, "a@a.com", "OLD", true));
        when(passwordEncoder.matches("newPassword1", "OLD")).thenReturn(true);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> service.reset(new PasswordResetRequest("raw", "newPassword1")));

        assertEquals(ErrorCode.SAME_PASSWORD, e.getErrorCode());
        verify(userMapper, never()).deletePasswordTokens(any());
        verify(userMapper, never()).updateUserPassword(any(), any());
    }

    @Test
    @DisplayName("성공하면 비밀번호를 바꾸고 재설정 링크와 모든 로그인을 지운다")
    void successChangesPasswordAndClearsSessions() {
        when(userMapper.selectPasswordTokenByHash(anyString())).thenReturn(token("h", LocalDateTime.now().plusMinutes(10)));
        when(userMapper.selectUserById(1L)).thenReturn(user(1L, "a@a.com", "OLD", true));
        when(passwordEncoder.matches("newPassword1", "OLD")).thenReturn(false);
        when(passwordEncoder.encode("newPassword1")).thenReturn("ENCODED");

        service.reset(new PasswordResetRequest("raw", "newPassword1"));

        verify(userMapper).updateUserPassword(1L, "ENCODED");
        verify(userMapper).deletePasswordTokens(1L);
        verify(userMapper).deleteRefreshToken(1L);
    }
}
