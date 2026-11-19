package com.flowspace.ai.service;

import com.flowspace.ai.dto.request.AiDraft;
import com.flowspace.ai.dto.response.AiApplyResponse;

// AI 초안 반영 서비스 (스프린트·작업·하위 작업·일정 일괄 등록)
public interface AiProjectApplyService {

    // 초안 일괄 등록 (관리자 이상, 한 트랜잭션)
    AiApplyResponse apply(Long workspaceId, AiDraft draft, String email);
}
