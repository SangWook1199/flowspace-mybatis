package com.flowspace.retrospective.service;

import java.util.List;

import com.flowspace.retrospective.dto.response.RetrospectiveListItem;
import com.flowspace.retrospective.dto.response.RetrospectiveResponse;

// 회고 기능의 목록이에요. 실제 동작은 service.impl의 RetrospectiveServiceImpl에 있어요.
public interface RetrospectiveService {

    // 회고 단건 조회
    RetrospectiveResponse getRetrospective(Long retrospectiveId, String email);

    // 워크스페이스 회고 목록 조회 (회고가 아직 없는 스프린트도 함께 반환)
    List<RetrospectiveListItem> getRetrospectives(Long workspaceId, String email);

    // 스프린트 회고 조회
    RetrospectiveResponse getBySprint(Long sprintId, String email);

    // 회고 만들기 (스프린트를 완료할 때 스프린트 서비스가 불러요). 이미 회고가 있으면 거절해요.
    void createRetrospective(Long sprintId, Long workspaceId, String sprintName, Long userId);
}
