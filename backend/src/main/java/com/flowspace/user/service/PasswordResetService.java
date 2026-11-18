package com.flowspace.user.service;

import com.flowspace.user.dto.request.PasswordForgotRequest;
import com.flowspace.user.dto.request.PasswordResetRequest;

// PasswordResetServiceImpl가 실제 동작을 맡아요. Controller와 다른 서비스는 이 인터페이스만 알아요.
public interface PasswordResetService {

    // 비밀번호가 없는 계정(소셜 로그인 전용)은 재설정할 비밀번호가 없어서 보내지 않아요.
    void requestReset(PasswordForgotRequest request);

    // 모든 기기의 로그인도 풀어요(비밀번호를 잃어버린 상황이라 남이 로그인해 있을 수도 있어서요).
    void reset(PasswordResetRequest request);
}
