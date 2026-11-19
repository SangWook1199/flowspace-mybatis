package com.flowspace.ai.service;

import com.flowspace.ai.dto.request.AiDraft;
import com.flowspace.ai.dto.request.AiProjectRequest;
import com.flowspace.ai.dto.response.AiApplyResponse;
import com.flowspace.ai.dto.response.AiJobResponse;
import com.flowspace.ai.dto.response.AiStatusResponse;

// AI 프로젝트 자동 구성 서비스 (초안 만들기 작업 시작·조회·취소, 확인한 초안 반영)
public interface AiDraftJobService {

    // AI 기능 사용 가능 여부
    AiStatusResponse status();

    // 초안 만들기 시작 (관리자 이상, 생성은 비동기)
    AiJobResponse start(Long workspaceId, AiProjectRequest request, String email);

    // 초안 만들기 작업 조회 (시작한 본인만)
    AiJobResponse get(Long workspaceId, String jobId, String email);

    // 초안 만들기 취소 (시작한 본인만)
    void cancel(Long workspaceId, String jobId, String email);

    // 확인한 초안 반영 (작업당 한 번)
    AiApplyResponse apply(Long workspaceId, String jobId, AiDraft draft, String email);
}
