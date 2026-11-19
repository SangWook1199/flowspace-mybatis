package com.flowspace.block.dto.response;

import java.util.List;

// @formatter:off

// 블록 일괄 동기화 응답 DTO
public record BlockSyncResponse(

    List<BlockSyncResult> blocks,

    // 저장 후 페이지 버전 (다음 저장의 baseVersion)
    Long version

) {

    public record BlockSyncResult(

        String clientId,
        BlockResponse block

    ) {
    }

}

// @formatter:on
