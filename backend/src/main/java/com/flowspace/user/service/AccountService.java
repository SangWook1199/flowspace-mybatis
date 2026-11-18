package com.flowspace.user.service;

import com.flowspace.user.dto.request.WithdrawRequest;
import com.flowspace.user.dto.response.WithdrawCheckResponse;

// AccountServiceImpl가 실제 동작을 맡아요. Controller와 다른 서비스는 이 인터페이스만 알아요.
public interface AccountService {

    // 탈퇴하면 내 워크스페이스가 어떻게 되는지 미리 알려줘요.
    WithdrawCheckResponse checkWithdraw(String email);

    // 회원 탈퇴
    void withdraw(WithdrawRequest request, String email);
}
