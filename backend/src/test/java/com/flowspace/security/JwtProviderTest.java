package com.flowspace.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.flowspace.common.enums.Provider;

// 토큰 종류 구분(access / refresh)과 변조·만료 거절을 확인해요.
class JwtProviderTest {

    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key-1234";

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpiration", 600_000L);
        jwtProvider.init();
    }

    @Test
    @DisplayName("access token은 access로만 인정되고 refresh로는 인정되지 않는다")
    void accessTokenIsOnlyAccess() {
        String token = jwtProvider.createAccessToken(7L, "user@example.com", Provider.LOCAL);

        assertTrue(jwtProvider.validateToken(token));
        assertTrue(jwtProvider.isAccessToken(token));
        assertFalse(jwtProvider.isRefreshToken(token));
        assertEquals(7L, jwtProvider.getUserId(token));
        assertEquals("user@example.com", jwtProvider.getEmail(token));
    }

    @Test
    @DisplayName("refresh token은 일반 API 인증(access)으로 쓸 수 없다")
    void refreshTokenIsNotAccess() {
        String token = jwtProvider.createRefreshToken(7L, jwtProvider.getRefreshTokenExpiration());

        assertTrue(jwtProvider.validateToken(token));
        assertTrue(jwtProvider.isRefreshToken(token));
        assertFalse(jwtProvider.isAccessToken(token));
        assertNull(jwtProvider.getEmail(token));
        assertEquals(7L, jwtProvider.getUserId(token));
    }

    @Test
    @DisplayName("만료된 토큰은 거절한다")
    void expiredTokenIsRejected() {
        String expired = jwtProvider.createRefreshToken(7L, -1_000L);

        assertFalse(jwtProvider.validateToken(expired));
        assertFalse(jwtProvider.isRefreshToken(expired));
        assertFalse(jwtProvider.isAccessToken(expired));
    }

    @Test
    @DisplayName("서명이 바뀐 토큰과 엉뚱한 문자열은 거절한다")
    void tamperedTokenIsRejected() {
        String token = jwtProvider.createAccessToken(7L, "user@example.com", Provider.LOCAL);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertFalse(jwtProvider.validateToken(tampered));
        assertFalse(jwtProvider.isAccessToken(tampered));
        assertFalse(jwtProvider.validateToken("not-a-jwt"));
        assertFalse(jwtProvider.isAccessToken(""));
    }

    @Test
    @DisplayName("다른 비밀키로 만든 토큰은 거절한다")
    void tokenFromOtherKeyIsRejected() {
        JwtProvider other = new JwtProvider();
        ReflectionTestUtils.setField(other, "secret", "another-secret-key-another-secret-key-another-1234");
        ReflectionTestUtils.setField(other, "accessTokenExpiration", 60_000L);
        ReflectionTestUtils.setField(other, "refreshTokenExpiration", 600_000L);
        other.init();

        assertFalse(jwtProvider.validateToken(other.createAccessToken(7L, "user@example.com", Provider.LOCAL)));
    }
}
