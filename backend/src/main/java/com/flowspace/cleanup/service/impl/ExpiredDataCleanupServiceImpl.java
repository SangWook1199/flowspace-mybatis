package com.flowspace.cleanup.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.cleanup.service.ExpiredDataCleanupService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpiredDataCleanupServiceImpl implements ExpiredDataCleanupService {

    // 정리 SQL
    private final CleanupMapper cleanupMapper;

    // 만료된 리프레시 토큰 삭제 (반환: 지운 개수)
    @Override
    public int deleteExpiredRefreshTokens() {
        return cleanupMapper.deleteExpiredRefreshTokens(LocalDateTime.now());
    }

    // 만료된 비밀번호 재설정 토큰 삭제 (반환: 지운 개수)
    @Override
    public int deleteExpiredPasswordResetTokens() {
        return cleanupMapper.deleteExpiredPasswordTokens(LocalDateTime.now());
    }

    // 만료된 이메일 인증 토큰 삭제 (반환: 지운 개수)
    @Override
    public int deleteExpiredEmailVerificationTokens() {
        return cleanupMapper.deleteExpiredEmailTokens(LocalDateTime.now());
    }

    // 보관 기간이 지난 대기 초대 삭제 (반환: 지운 개수)
    @Override
    public int deleteStaleInvites(int retentionDays) {
        return cleanupMapper.deleteStaleInvites(LocalDateTime.now().minusDays(retentionDays));
    }
}
