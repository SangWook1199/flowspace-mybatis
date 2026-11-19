package com.flowspace.user.service;

import com.flowspace.user.dto.request.WithdrawRequest;
import com.flowspace.user.dto.response.WithdrawCheckResponse;

// 계정 서비스
public interface AccountService {

    // 탈퇴 시 워크스페이스 처리 방식 미리 확인
    WithdrawCheckResponse checkWithdraw(String email);

    // 회원 탈퇴
    void withdraw(WithdrawRequest request, String email);
}
