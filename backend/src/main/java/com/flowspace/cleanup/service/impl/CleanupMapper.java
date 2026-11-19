package com.flowspace.cleanup.service.impl;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;

// 만료 데이터 정리 Mapper (Cleanup_SQL.xml)
@Mapper
public interface CleanupMapper {

    // 만료된 리프레시 토큰 삭제
    int deleteExpiredRefreshTokens(LocalDateTime now);

    // 만료된 비밀번호 재설정 토큰 삭제
    int deleteExpiredPasswordTokens(LocalDateTime now);

    // 만료된 이메일 인증 토큰 삭제
    int deleteExpiredEmailTokens(LocalDateTime now);

    // 이 시각 이전에 보낸 대기 초대 삭제
    int deleteStaleInvites(LocalDateTime before);
}
