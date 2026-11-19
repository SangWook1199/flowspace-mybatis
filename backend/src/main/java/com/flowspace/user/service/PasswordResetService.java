package com.flowspace.user.service;

import com.flowspace.user.dto.request.PasswordForgotRequest;
import com.flowspace.user.dto.request.PasswordResetRequest;

// 비밀번호 재설정 서비스
public interface PasswordResetService {

    // 비밀번호가 없는 계정(소셜 전용)은 발송하지 않음
    void requestReset(PasswordForgotRequest request);

    // 모든 기기 로그인 해제 (비밀번호 분실 상황 고려)
    void reset(PasswordResetRequest request);
}
