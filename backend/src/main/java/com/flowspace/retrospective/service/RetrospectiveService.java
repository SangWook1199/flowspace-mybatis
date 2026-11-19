package com.flowspace.retrospective.service;

import java.util.List;

import com.flowspace.retrospective.dto.response.RetrospectiveListItem;
import com.flowspace.retrospective.dto.response.RetrospectiveResponse;

// 회고 서비스
public interface RetrospectiveService {

    // 회고 단건 조회
    RetrospectiveResponse getRetrospective(Long retrospectiveId, String email);

    // 워크스페이스 회고 목록 조회 (회고가 아직 없는 스프린트도 함께 반환)
    List<RetrospectiveListItem> getRetrospectives(Long workspaceId, String email);

    // 스프린트 회고 조회
    RetrospectiveResponse getBySprint(Long sprintId, String email);

    // 회고 생성 (스프린트 완료 시 호출, 이미 있으면 RETROSPECTIVE_ALREADY_EXISTS)
    void createRetrospective(Long sprintId, Long workspaceId, String sprintName, Long userId);
}
