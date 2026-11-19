package com.flowspace.cleanup.service;

// 만료 데이터 정리 서비스 (토큰, 오래된 대기 초대, CleanupScheduler가 호출)
public interface ExpiredDataCleanupService {

    // 만료된 리프레시 토큰 삭제
    int deleteExpiredRefreshTokens();

    // 만료된 비밀번호 재설정 토큰 삭제
    int deleteExpiredPasswordResetTokens();

    // 만료된 이메일 인증 토큰 삭제
    int deleteExpiredEmailVerificationTokens();

    // 보관 기간이 지난 대기 초대 삭제
    int deleteStaleInvites(int retentionDays);
}
