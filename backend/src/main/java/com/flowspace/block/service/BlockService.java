package com.flowspace.block.service;

import java.util.List;

import com.flowspace.block.dto.request.BlockCreateRequest;
import com.flowspace.block.dto.request.BlockIndentRequest;
import com.flowspace.block.dto.request.BlockReorderRequest;
import com.flowspace.block.dto.request.BlockSyncRequest;
import com.flowspace.block.dto.request.BlockUpdateRequest;
import com.flowspace.block.dto.response.BlockResponse;
import com.flowspace.block.dto.response.BlockSyncResponse;
import org.springframework.web.multipart.MultipartFile;

// 블록 서비스
public interface BlockService {

    // 블록 생성
    BlockResponse createBlock(Long pageId, BlockCreateRequest request, String email);

    // 페이지 블록 목록 조회
    List<BlockResponse> getBlocks(Long pageId, String email);

    // 블록 수정
    BlockResponse updateBlock(Long blockId, BlockUpdateRequest request, String email);

    // 블록 삭제
    void deleteBlock(Long blockId, String email);

    // 블록 순서 변경
    void reorderBlocks(Long pageId, BlockReorderRequest request, String email);

    // 블록 이동
    BlockResponse moveBlock(Long blockId, BlockIndentRequest request, String email);

    // 블록 일괄 동기화 (목록 순서 = 블록 순서, 목록에 없는 기존 블록은 삭제)
    BlockSyncResponse syncBlocks(Long pageId, BlockSyncRequest request, String email);

    // 블록 이미지 업로드
    BlockResponse uploadImage(Long blockId, MultipartFile multipartFile, String email);

    // 블록 이미지 삭제
    BlockResponse deleteImage(Long blockId, String email);
}
