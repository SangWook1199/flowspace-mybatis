package com.flowspace.block.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowspace.block.dto.request.BlockCreateRequest;
import com.flowspace.block.dto.request.BlockIndentRequest;
import com.flowspace.block.dto.request.BlockOrderItem;
import com.flowspace.block.dto.request.BlockReorderRequest;
import com.flowspace.block.dto.request.BlockSyncRequest;
import com.flowspace.block.dto.request.BlockSyncRequest.BlockSyncItem;
import com.flowspace.block.dto.request.BlockUpdateRequest;
import com.flowspace.block.dto.response.BlockResponse;
import com.flowspace.block.dto.response.BlockSyncResponse;
import com.flowspace.block.dto.response.BlockSyncResponse.BlockSyncResult;
import com.flowspace.block.service.BlockService;
import com.flowspace.block.service.BlockVO;
import com.flowspace.common.enums.BlockType;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.file.service.FileService;
import com.flowspace.file.service.FileVO;
import com.flowspace.page.service.PageVO;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BlockServiceImpl implements BlockService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 블록 SQL
    private final BlockMapper blockMapper;
    private final WorkspaceService workspaceService;
    private final FileService fileService;

    // 블록 생성 (페이지 맨 뒤)
    @Override
    public BlockResponse createBlock(Long pageId, BlockCreateRequest request, String email) {

        Long userId = findUserId(email);
        PageVO page = findActivePage(pageId);

        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        if (request.parentBlockId() != null) {
            BlockVO parent = findBlock(request.parentBlockId());

            if (!parent.getPageId().equals(pageId)) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }
        }

        BlockType type = request.type() == null ? BlockType.TEXT : request.type();

        validateContent(request.content());

        BlockVO block = new BlockVO();
        block.setPageId(pageId);
        block.setParentBlockId(request.parentBlockId());
        block.setType(type);
        block.setContent(request.content());
        block.setTaskId(linkedTaskId(type, request.taskId(), page.getWorkspaceId()));
        block.setEventId(linkedEventId(type, request.eventId(), page.getWorkspaceId()));
        block.setSprintId(linkedSprintId(type, request.sprintId(), page.getWorkspaceId()));
        block.setPosition(BigDecimal.valueOf(blockMapper.selectBlockCount(pageId)));
        block.setCreatedBy(userId);

        blockMapper.insertBlock(block);

        blockMapper.updatePageVersion(pageId);

        return BlockResponse.from(findBlock(block.getBlockId()));
    }

    // 페이지 블록 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<BlockResponse> getBlocks(Long pageId, String email) {

        PageVO page = findActivePage(pageId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        return blockMapper.selectBlockList(pageId).stream().map(BlockResponse::from).toList();
    }

    // 블록 수정
    @Override
    public BlockResponse updateBlock(Long blockId, BlockUpdateRequest request, String email) {

        Long userId = findUserId(email);
        BlockVO block = findBlock(blockId);

        workspaceService.getWorkspace(block.getWorkspaceId(), email);
        requireActivePage(block);

        validateContent(request.content());

        blockMapper.updateBlockContent(blockId, request.type() == null ? BlockType.TEXT : request.type(),
            request.content(), userId);

        blockMapper.updatePageVersion(block.getPageId());

        return BlockResponse.from(findBlock(blockId));
    }

    // 블록 삭제
    @Override
    public void deleteBlock(Long blockId, String email) {

        BlockVO block = findBlock(blockId);

        workspaceService.getWorkspace(block.getWorkspaceId(), email);
        requireActivePage(block);

        if (block.getImageFileId() != null) {
            fileService.delete(block.getImageFileId());
        }

        blockMapper.updatePageVersion(block.getPageId());

        blockMapper.deleteBlock(blockId);
    }

    // 블록 순서 변경
    @Override
    public void reorderBlocks(Long pageId, BlockReorderRequest request, String email) {

        PageVO page = findActivePage(pageId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        List<Long> blockIds = request.blocks().stream().map(BlockOrderItem::blockId).toList();

        Map<Long, Long> pageIdByBlock = new HashMap<>();

        for (BlockVO block : blockMapper.selectBlockListByIds(blockIds)) {
            pageIdByBlock.put(block.getBlockId(), block.getPageId());
        }

        for (Long blockId : blockIds) {

            if (!pageIdByBlock.containsKey(blockId)) {
                throw new FlowSpaceException(ErrorCode.BLOCK_NOT_FOUND);
            }

            if (!pageIdByBlock.get(blockId).equals(pageId)) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }
        }

        for (BlockOrderItem item : request.blocks()) {
            blockMapper.updateBlockPosition(item.blockId(), BigDecimal.valueOf(item.position()));
        }

        blockMapper.updatePageVersion(pageId);
    }

    // 블록 이동 (들여쓰기·내어쓰기)
    @Override
    public BlockResponse moveBlock(Long blockId, BlockIndentRequest request, String email) {

        BlockVO block = findBlock(blockId);

        workspaceService.getWorkspace(block.getWorkspaceId(), email);
        requireActivePage(block);

        Long parentId = request.parentBlockId();

        if (parentId != null) {
            BlockVO parent = findBlock(parentId);

            if (!parent.getPageId().equals(block.getPageId())) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }

            validateParentHierarchy(blockId, parentId);
        }

        blockMapper.updateBlockParent(blockId, parentId, BigDecimal.valueOf(request.position()));

        blockMapper.updatePageVersion(block.getPageId());

        return BlockResponse.from(findBlock(blockId));
    }

    // 블록 순환 참조 검증 (상위 블록을 따라 올라가며 자기 자신 확인)
    private void validateParentHierarchy(Long blockId, Long parentId) {

        Long current = parentId;

        while (current != null) {

            if (current.equals(blockId)) {
                throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_PARENT);
            }

            current = blockMapper.selectParentBlockId(current);
        }
    }

    // 블록 일괄 동기화 (목록 순서 = 블록 순서, 목록에 없는 기존 블록은 삭제)
    @Override
    public BlockSyncResponse syncBlocks(Long pageId, BlockSyncRequest request, String email) {

        Long userId = findUserId(email);

        PageVO page = blockMapper.selectBlockPageForUpdate(pageId);

        if (page == null) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }

        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        if (request.baseVersion() != null && !request.baseVersion().equals(page.getVersion())) {
            throw new FlowSpaceException(ErrorCode.PAGE_VERSION_CONFLICT);
        }

        List<BlockSyncItem> items = request.blocks();

        validateSyncItems(items);

        Map<Long, BlockVO> existing = new HashMap<>();
        for (BlockVO block : blockMapper.selectBlockList(pageId)) {
            existing.put(block.getBlockId(), block);
        }

        Map<String, Long> idByClientId = new HashMap<>();
        Set<Long> keptIds = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {

            BlockSyncItem item = items.get(i);
            BlockType type = item.type() == null ? BlockType.TEXT : item.type();

            validateContent(item.content());

            Long parentId = item.parentClientId() == null ? null : idByClientId.get(item.parentClientId());
            BigDecimal position = BigDecimal.valueOf(i);

            if (item.blockId() != null) {

                BlockVO block = existing.get(item.blockId());

                if (block == null) {
                    throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
                }

                keptIds.add(block.getBlockId());

                boolean placeChanged = !Objects.equals(parentId, block.getParentBlockId())
                    || block.getPosition() == null || block.getPosition().compareTo(position) != 0;

                if (block.getType() == BlockType.DATABASE) {

                    if (type != BlockType.DATABASE) {
                        throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
                    }

                    if (placeChanged) {
                        blockMapper.updateBlockParent(block.getBlockId(), parentId, position);
                    }

                } else {

                    if (type == BlockType.DATABASE) {
                        throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
                    }

                    Long taskId = linkedTaskId(type, item.taskId(), page.getWorkspaceId());
                    Long eventId = linkedEventId(type, item.eventId(), page.getWorkspaceId());
                    Long sprintId = linkedSprintId(type, item.sprintId(), page.getWorkspaceId());

                    boolean contentChanged = type != block.getType() || !sameContent(item.content(), block.getContent())
                        || !Objects.equals(taskId, block.getTaskId()) || !Objects.equals(eventId, block.getEventId())
                        || !Objects.equals(sprintId, block.getSprintId());

                    if (contentChanged) {
                        block.setType(type);
                        block.setContent(item.content());
                        block.setTaskId(taskId);
                        block.setEventId(eventId);
                        block.setSprintId(sprintId);
                        block.setParentBlockId(parentId);
                        block.setPosition(position);
                        block.setUpdatedBy(userId);

                        blockMapper.updateBlockSync(block);
                    } else if (placeChanged) {
                        blockMapper.updateBlockParent(block.getBlockId(), parentId, position);
                    }
                }

                idByClientId.put(item.clientId(), block.getBlockId());

            } else {

                if (type == BlockType.DATABASE) {
                    throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
                }

                BlockVO block = new BlockVO();
                block.setPageId(pageId);
                block.setParentBlockId(parentId);
                block.setType(type);
                block.setContent(item.content());
                block.setTaskId(linkedTaskId(type, item.taskId(), page.getWorkspaceId()));
                block.setEventId(linkedEventId(type, item.eventId(), page.getWorkspaceId()));
                block.setSprintId(linkedSprintId(type, item.sprintId(), page.getWorkspaceId()));
                block.setPosition(position);
                block.setCreatedBy(userId);

                blockMapper.insertBlock(block);

                idByClientId.put(item.clientId(), block.getBlockId());
            }
        }

        List<BlockVO> removed = existing.values().stream().filter(block -> !keptIds.contains(block.getBlockId()))
            .toList();

        if (!removed.isEmpty()) {

            List<Long> removedIds = removed.stream().map(BlockVO::getBlockId).toList();

            blockMapper.updateBlockParentsCleared(removedIds);

            for (BlockVO block : removed) {
                if (block.getImageFileId() != null) {
                    fileService.delete(block.getImageFileId());
                }
            }

            blockMapper.deleteBlocks(removedIds);
        }

        blockMapper.updatePageVersion(pageId);

        Map<Long, BlockVO> saved = new HashMap<>();
        for (BlockVO block : blockMapper.selectBlockList(pageId)) {
            saved.put(block.getBlockId(), block);
        }

        List<BlockSyncResult> results = new ArrayList<>();

        for (BlockSyncItem item : items) {
            BlockVO block = saved.get(idByClientId.get(item.clientId()));
            results.add(new BlockSyncResult(item.clientId(), BlockResponse.from(block)));
        }

        return new BlockSyncResponse(results, page.getVersion() + 1);
    }

    // 동기화 요청 검증 (clientId 중복, 부모는 자식보다 앞에 위치)
    private void validateSyncItems(List<BlockSyncItem> items) {

        Set<String> seenClientIds = new HashSet<>();
        Set<Long> seenBlockIds = new HashSet<>();

        for (BlockSyncItem item : items) {

            if (!seenClientIds.add(item.clientId())) {
                throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
            }

            if (item.blockId() != null && !seenBlockIds.add(item.blockId())) {
                throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
            }

            if (item.parentClientId() != null && !seenClientIds.contains(item.parentClientId())) {
                throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
            }

            if (item.parentClientId() != null && item.parentClientId().equals(item.clientId())) {
                throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_PARENT);
            }
        }
    }

    // 블록 content JSON 형식 검증
    private void validateContent(String content) {

        if (content == null || content.isBlank()) {
            return;
        }

        try {
            OBJECT_MAPPER.readTree(content);
        } catch (JsonProcessingException e) {
            throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_CONTENT);
        }
    }

    // 내용 비교 (JSON 칸은 공백·키 순서가 정리되어 저장되므로 JSON 값으로 비교)
    private boolean sameContent(String a, String b) {

        boolean aEmpty = a == null || a.isBlank();
        boolean bEmpty = b == null || b.isBlank();

        if (aEmpty || bEmpty) {
            return aEmpty && bEmpty;
        }

        try {
            return OBJECT_MAPPER.readTree(a).equals(OBJECT_MAPPER.readTree(b));
        } catch (JsonProcessingException e) {
            return a.equals(b);
        }
    }

    // ---------- 업무 블록 연결 대상 확인 (종류가 맞을 때만 번호 반환) ----------

    private Long linkedTaskId(BlockType type, Long taskId, Long workspaceId) {

        if (type != BlockType.TASK) {
            return null;
        }

        if (taskId == null) {
            throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
        }

        checkSameWorkspace(blockMapper.selectTaskWorkspaceId(taskId), workspaceId, ErrorCode.TASK_NOT_FOUND);

        return taskId;
    }

    private Long linkedEventId(BlockType type, Long eventId, Long workspaceId) {

        if (type != BlockType.EVENT) {
            return null;
        }

        if (eventId == null) {
            throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
        }

        checkSameWorkspace(blockMapper.selectEventWorkspaceId(eventId), workspaceId, ErrorCode.EVENT_NOT_FOUND);

        return eventId;
    }

    private Long linkedSprintId(BlockType type, Long sprintId, Long workspaceId) {

        if (type != BlockType.SPRINT) {
            return null;
        }

        if (sprintId == null) {
            throw new FlowSpaceException(ErrorCode.INVALID_BLOCK_SYNC);
        }

        checkSameWorkspace(blockMapper.selectSprintWorkspaceId(sprintId), workspaceId, ErrorCode.SPRINT_NOT_FOUND);

        return sprintId;
    }

    // 대상이 없으면(워크스페이스 번호가 null) notFound, 다른 워크스페이스 것이면 ACCESS_DENIED
    private void checkSameWorkspace(Long targetWorkspaceId, Long workspaceId, ErrorCode notFound) {

        if (targetWorkspaceId == null) {
            throw new FlowSpaceException(notFound);
        }

        if (!targetWorkspaceId.equals(workspaceId)) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }
    }

    // 블록 이미지 업로드 (새 파일 저장 후 기존 파일 삭제)
    @Override
    public BlockResponse uploadImage(Long blockId, MultipartFile multipartFile, String email) {

        BlockVO block = findBlock(blockId);

        workspaceService.getWorkspace(block.getWorkspaceId(), email);
        requireActivePage(block);

        FileVO uploaded = block.getType() == BlockType.FILE
            ? fileService.upload(multipartFile, block.getWorkspaceId(), email)
            : fileService.uploadImage(multipartFile, block.getWorkspaceId(), email);

        if (block.getImageFileId() != null) {
            fileService.delete(block.getImageFileId());
        }

        blockMapper.updateBlockImage(blockId, uploaded.getFileId());

        return BlockResponse.from(findBlock(blockId));
    }

    // 블록 이미지 삭제
    @Override
    public BlockResponse deleteImage(Long blockId, String email) {

        BlockVO block = findBlock(blockId);

        workspaceService.getWorkspace(block.getWorkspaceId(), email);
        requireActivePage(block);

        if (block.getImageFileId() != null) {
            fileService.delete(block.getImageFileId());
            blockMapper.updateBlockImage(blockId, null);
        }

        return BlockResponse.from(findBlock(blockId));
    }

    // ---------- 조회 + 없으면 예외 ----------

    private Long findUserId(String email) {

        Long userId = blockMapper.selectUserIdByEmail(email);

        if (userId == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        return userId;
    }

    // 휴지통에 있지 않은 페이지
    private PageVO findActivePage(Long pageId) {

        PageVO page = blockMapper.selectBlockPage(pageId);

        if (page == null || Boolean.TRUE.equals(page.getIsDeleted())) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }

        return page;
    }

    private BlockVO findBlock(Long blockId) {

        BlockVO block = blockMapper.selectBlockById(blockId);

        if (block == null) {
            throw new FlowSpaceException(ErrorCode.BLOCK_NOT_FOUND);
        }

        return block;
    }

    // 휴지통 페이지의 블록 수정 차단
    private void requireActivePage(BlockVO block) {

        if (Boolean.TRUE.equals(block.getPageDeleted())) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }
    }
}
