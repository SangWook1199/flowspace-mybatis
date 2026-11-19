package com.flowspace.search.service;

import com.flowspace.search.dto.response.SearchResponse;

// 통합 검색 서비스
public interface SearchService {

    // 통합 검색
    SearchResponse search(Long workspaceId, String query, String email);
}
