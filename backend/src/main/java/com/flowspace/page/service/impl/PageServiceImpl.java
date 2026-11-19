package com.flowspace.page.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowspace.activity.service.ActivityService;
import com.flowspace.block.dto.response.BlockResponse;
import com.flowspace.block.service.BlockService;
import com.flowspace.block.service.BlockVO;
import com.flowspace.comment.dto.response.CommentResponse;
import com.flowspace.comment.service.CommentService;
import com.flowspace.database.service.BlockDatabaseColumnVO;
import com.flowspace.database.service.BlockDatabaseRowVO;
import com.flowspace.database.service.BlockDatabaseVO;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.entity.enums.BlockType;
import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.file.service.FileService;
import com.flowspace.file.service.FileVO;
import com.flowspace.page.dto.request.PageCoverUpdateRequest;
import com.flowspace.page.dto.request.PageCreateRequest;
import com.flowspace.page.dto.request.PageDuplicateRequest;
import com.flowspace.page.dto.request.PageReorderRequest;
import com.flowspace.page.dto.request.PageUpdateRequest;
import com.flowspace.page.dto.response.PageDetailResponse;
import com.flowspace.page.dto.response.PageResponse;
import com.flowspace.page.service.PageService;
import com.flowspace.page.service.PageVO;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PageServiceImpl implements PageService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 페이지 SQL
    private final PageMapper pageMapper;

    // 페이지 복제 SQL
    private final PageCopyMapper pageCopyMapper;

    private final WorkspaceService workspaceService;
    private final ActivityService activityService;
    private final CommentService commentService;
    private final FileService fileService;

    // 상세 조회용 블록 목록
    private final BlockService blockService;

    // 페이지 생성
    @Override
    public PageResponse createPage(Long workspaceId, PageCreateRequest request, String email) {

        workspaceService.getWorkspace(workspaceId, email);

        Long parentId = request.parentPageId();

        if (parentId != null) {
            PageVO parent = findActivePage(parentId);

            if (!parent.getWorkspaceId().equals(workspaceId)) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }
        }

        PageVO page = new PageVO();
        page.setWorkspaceId(workspaceId);
        page.setParentPageId(parentId);
        page.setTitle(request.title());
        page.setIcon(request.icon());
        page.setPosition(pageMapper.selectSiblingCount(workspaceId, parentId));
        page.setCreatedBy(findUserId(email));

        pageMapper.insertPage(page);

        activityService.log(workspaceId, page.getCreatedBy(), ActivityType.PAGE_CREATED, ActivityTargetType.PAGE,
            page.getPageId());

        return toResponse(page.getPageId());
    }

    // 페이지 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<PageResponse> getPages(Long workspaceId, String email) {

        workspaceService.getWorkspace(workspaceId, email);

        return pageMapper.selectPageList(workspaceId, false).stream().map(PageResponse::from).toList();
    }

    // 페이지 단건 조회
    @Transactional(readOnly = true)
    @Override
    public PageResponse getPage(Long pageId, String email) {

        PageVO page = findActivePage(pageId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        return PageResponse.from(page);
    }

    // 페이지 수정 (상위 페이지가 null이면 최상위로 이동)
    @Override
    public PageResponse updatePage(Long pageId, PageUpdateRequest request, String email) {

        PageVO page = findActivePage(pageId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        Long parentId = request.parentPageId();

        if (parentId != null) {
            PageVO parent = findActivePage(parentId);

            if (!parent.getWorkspaceId().equals(page.getWorkspaceId())) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }

            validateParentHierarchy(pageId, parentId);
        }

        page.setTitle(request.title());
        page.setIcon(request.icon());
        page.setParentPageId(parentId);

        pageMapper.updatePage(page);

        return toResponse(pageId);
    }

    // 페이지 삭제 (휴지통으로 보내기)
    @Override
    public void deletePage(Long pageId, String email) {

        PageVO page = findActivePage(pageId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        List<PageVO> subtree = pageMapper.selectPageSubtree(pageId);

        List<Long> subtreeIds = subtree.stream().map(PageVO::getPageId).toList();

        List<Long> targetIds = subtree.stream().filter(target -> !Boolean.TRUE.equals(target.getIsDeleted()))
            .map(PageVO::getPageId).toList();

        pageMapper.deleteOutsideDatabaseRows(targetIds, subtreeIds);

        pageMapper.updatePagesTrashed(targetIds, LocalDateTime.now());
    }

    // 휴지통 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<PageResponse> getTrash(Long workspaceId, String email) {

        workspaceService.getWorkspace(workspaceId, email);

        return pageMapper.selectPageList(workspaceId, true).stream().map(PageResponse::from).toList();
    }

    // 휴지통 페이지 복원
    @Override
    public PageResponse restorePage(Long pageId, String email) {

        PageVO page = findPage(pageId);

        if (!Boolean.TRUE.equals(page.getIsDeleted())) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }

        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        if (Boolean.TRUE.equals(page.getParentDeleted())) {
            pageMapper.updatePageParent(pageId, null, pageMapper.selectSiblingCount(page.getWorkspaceId(), null));
        }

        LocalDateTime deletedAt = page.getDeletedAt();

        List<Long> targetIds = pageMapper.selectPageSubtree(pageId).stream()
            .filter(target -> Boolean.TRUE.equals(target.getIsDeleted())
                && Objects.equals(target.getDeletedAt(), deletedAt))
            .map(PageVO::getPageId).toList();

        pageMapper.updatePagesRestored(targetIds);

        return toResponse(pageId);
    }

    // 휴지통 페이지 영구 삭제 (관리자 이상, 하위 페이지 포함, 회고 페이지가 있으면 거절)
    @Override
    public void deletePagePermanently(Long pageId, String email) {

        PageVO page = findPage(pageId);

        if (!Boolean.TRUE.equals(page.getIsDeleted())) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }

        requireAdmin(page.getWorkspaceId(), email);

        List<PageVO> subtree = pageMapper.selectPageSubtree(pageId);

        List<Long> subtreeIds = subtree.stream().map(PageVO::getPageId).toList();

        if (!pageMapper.selectRetrospectivePageIds(subtreeIds).isEmpty()) {
            throw new FlowSpaceException(ErrorCode.RETROSPECTIVE_PAGE_PROTECTED);
        }

        purgePages(subtree);
    }

    // 휴지통 비우기 (관리자 이상, 회고 페이지는 남겨둠)
    @Override
    public void emptyTrash(Long workspaceId, String email) {

        requireAdmin(workspaceId, email);

        purgeTrashed(pageMapper.selectPageList(workspaceId, true));
    }

    // 보관 기간이 지난 휴지통 페이지 영구 삭제 (스케줄러용, 반환: 대상 수)
    @Override
    public int purgeExpiredTrash(int retentionDays) {

        List<PageVO> expired = pageMapper.selectExpiredTrashList(LocalDateTime.now().minusDays(retentionDays));

        purgeTrashed(expired);

        return expired.size();
    }

    // 휴지통 페이지 영구 삭제 (회고 페이지와 휴지통 안의 그 상위 페이지는 유지)
    private void purgeTrashed(List<PageVO> trashed) {

        if (trashed.isEmpty()) {
            return;
        }

        Map<Long, PageVO> trashedById = new HashMap<>();

        for (PageVO page : trashed) {
            trashedById.put(page.getPageId(), page);
        }

        Set<Long> protectedIds = new HashSet<>();

        for (Long retrospectiveId : pageMapper.selectRetrospectivePageIds(new ArrayList<>(trashedById.keySet()))) {
            Long current = retrospectiveId;

            while (current != null && trashedById.containsKey(current)) {
                protectedIds.add(current);
                current = trashedById.get(current).getParentPageId();
            }
        }

        purgePages(trashed.stream().filter(page -> !protectedIds.contains(page.getPageId())).toList());
    }

    // 페이지 영구 삭제 (가리키는 데이터베이스 행 삭제, 하위 페이지부터 삭제, 커버·블록 이미지 파일 정리)
    private void purgePages(List<PageVO> pages) {

        if (pages.isEmpty()) {
            return;
        }

        List<Long> pageIds = pages.stream().map(PageVO::getPageId).toList();
        List<Long> fileIds = pageMapper.selectPageFileIds(pageIds);

        pageMapper.deleteDatabaseRowsByPages(pageIds);

        for (PageVO page : childrenFirst(pages)) {
            pageMapper.deletePage(page.getPageId());
        }

        for (Long fileId : fileIds) {
            fileService.delete(fileId);
        }
    }

    // 하위 페이지가 먼저 오는 순서 (목록 안의 상위 페이지 수가 많은 순)
    private List<PageVO> childrenFirst(List<PageVO> pages) {

        Map<Long, PageVO> byId = new HashMap<>();

        for (PageVO page : pages) {
            byId.put(page.getPageId(), page);
        }

        Map<Long, Integer> depths = new HashMap<>();

        for (PageVO page : pages) {
            int depth = 0;
            Long current = page.getParentPageId();

            while (current != null && byId.containsKey(current)) {
                depth++;
                current = byId.get(current).getParentPageId();
            }

            depths.put(page.getPageId(), depth);
        }

        return pages.stream()
            .sorted(Comparator.comparing((PageVO page) -> depths.get(page.getPageId())).reversed())
            .toList();
    }

    // 페이지 순서 변경 (같은 상위 페이지 아래의 페이지만)
    @Override
    public void reorderPages(Long workspaceId, PageReorderRequest request, String email) {

        workspaceService.getWorkspace(workspaceId, email);

        List<Long> pageIds = request.pageIds();

        if (new HashSet<>(pageIds).size() != pageIds.size()) {
            throw new FlowSpaceException(ErrorCode.INVALID_PAGE_REORDER);
        }

        Map<Long, PageVO> pages = new HashMap<>();

        for (PageVO page : pageMapper.selectPageListByIds(pageIds)) {
            pages.put(page.getPageId(), page);
        }

        Long parentId = null;

        for (int i = 0; i < pageIds.size(); i++) {

            PageVO page = pages.get(pageIds.get(i));

            if (page == null || Boolean.TRUE.equals(page.getIsDeleted())) {
                throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
            }

            if (!page.getWorkspaceId().equals(workspaceId)) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }

            if (i == 0) {
                parentId = page.getParentPageId();
            } else if (!Objects.equals(parentId, page.getParentPageId())) {
                throw new FlowSpaceException(ErrorCode.INVALID_PAGE_REORDER);
            }
        }

        for (int i = 0; i < pageIds.size(); i++) {
            pageMapper.updatePagePosition(pageIds.get(i), i);
        }
    }

    // 페이지 복제 (휴지통이 아닌 하위 페이지, 블록, 데이터베이스까지 함께 복사)
    @Override
    public PageResponse duplicatePage(Long pageId, PageDuplicateRequest request, String email) {

        Long userId = findUserId(email);
        PageVO source = findActivePage(pageId);
        Long workspaceId = source.getWorkspaceId();

        workspaceService.getWorkspace(workspaceId, email);

        List<PageVO> sources = activeSubtree(pageId);

        Long newParentId = source.getParentPageId();

        if (request != null && request.parentPageId() != null) {
            PageVO newParent = findActivePage(request.parentPageId());

            if (!newParent.getWorkspaceId().equals(workspaceId)) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }

            if (sources.stream().anyMatch(target -> target.getPageId().equals(newParent.getPageId()))) {
                throw new FlowSpaceException(ErrorCode.INVALID_PAGE_PARENT);
            }

            newParentId = newParent.getPageId();
        }

        Map<Long, Long> pageIdMap = new LinkedHashMap<>();

        for (PageVO target : sources) {
            boolean isRoot = target.getPageId().equals(pageId);
            Long parentId = isRoot ? newParentId : pageIdMap.get(target.getParentPageId());

            PageVO copy = new PageVO();
            copy.setWorkspaceId(workspaceId);
            copy.setParentPageId(parentId);
            copy.setTitle(target.getTitle());
            copy.setIcon(target.getIcon());
            copy.setCoverFileId(copyFile(target.getCoverFileId(), workspaceId, userId));
            copy.setPosition(isRoot ? pageMapper.selectSiblingCount(workspaceId, parentId) : target.getPosition());
            copy.setCreatedBy(userId);

            pageMapper.insertPage(copy);

            pageIdMap.put(target.getPageId(), copy.getPageId());
        }

        for (PageVO target : sources) {
            copyBlocks(target.getPageId(), pageIdMap.get(target.getPageId()), pageIdMap, workspaceId, userId);
        }

        Long rootCopyId = pageIdMap.get(pageId);

        activityService.log(workspaceId, userId, ActivityType.PAGE_CREATED, ActivityTargetType.PAGE, rootCopyId);

        return toResponse(rootCopyId);
    }

    // 휴지통이 아닌 본인과 하위 페이지 (휴지통 페이지 아래는 제외, 상위가 먼저)
    private List<PageVO> activeSubtree(Long pageId) {

        Set<Long> includedIds = new HashSet<>();
        List<PageVO> result = new ArrayList<>();

        for (PageVO page : pageMapper.selectPageSubtree(pageId)) {
            boolean reachable = page.getDepth() == 0 || includedIds.contains(page.getParentPageId());

            if (reachable && !Boolean.TRUE.equals(page.getIsDeleted())) {
                includedIds.add(page.getPageId());
                result.add(page);
            }
        }

        return result;
    }

    // 페이지의 블록 복사 (이미지 파일 복사, 하위 페이지 링크 교체, 상위 블록 연결)
    private void copyBlocks(Long sourcePageId, Long copyPageId, Map<Long, Long> pageIdMap, Long workspaceId,
        Long userId) {

        List<BlockVO> blocks = pageCopyMapper.selectBlockList(sourcePageId);

        Map<Long, Long> blockIdMap = new HashMap<>();

        for (BlockVO block : blocks) {
            BlockVO copy = new BlockVO();
            copy.setPageId(copyPageId);
            copy.setTaskId(block.getTaskId());
            copy.setEventId(block.getEventId());
            copy.setSprintId(block.getSprintId());
            copy.setImageFileId(copyFile(block.getImageFileId(), workspaceId, userId));
            copy.setType(block.getType());
            copy.setPosition(block.getPosition());
            copy.setContent(remapPageId(block.getContent(), pageIdMap));
            copy.setCreatedBy(userId);

            pageCopyMapper.insertBlock(copy);

            blockIdMap.put(block.getBlockId(), copy.getBlockId());

            if (block.getType() == BlockType.DATABASE) {
                copyDatabase(block.getBlockId(), copy.getBlockId(), pageIdMap);
            }
        }

        for (BlockVO block : blocks) {
            if (block.getParentBlockId() != null) {
                pageCopyMapper.updateBlockParent(blockIdMap.get(block.getBlockId()),
                    blockIdMap.get(block.getParentBlockId()));
            }
        }
    }

    // 데이터베이스 복사 (컬럼, 옵션, 행, 셀, 행 페이지는 복제본으로 교체)
    private void copyDatabase(Long sourceBlockId, Long copyBlockId, Map<Long, Long> pageIdMap) {

        BlockDatabaseVO source = pageCopyMapper.selectDatabaseByBlock(sourceBlockId);

        if (source == null) {
            return;
        }

        BlockDatabaseVO database = new BlockDatabaseVO();
        database.setBlockId(copyBlockId);
        database.setTitle(source.getTitle());
        database.setViewType(source.getViewType());

        pageCopyMapper.insertDatabase(database);

        Map<Long, Long> columnIdMap = new HashMap<>();

        for (BlockDatabaseColumnVO column : pageCopyMapper.selectColumnList(source.getDatabaseId())) {
            BlockDatabaseColumnVO copy = new BlockDatabaseColumnVO();
            copy.setDatabaseId(database.getDatabaseId());
            copy.setName(column.getName());
            copy.setType(column.getType());
            copy.setPosition(column.getPosition());
            copy.setWidth(column.getWidth());

            pageCopyMapper.insertColumn(copy);

            columnIdMap.put(column.getColumnId(), copy.getColumnId());

            pageCopyMapper.insertOptionCopies(column.getColumnId(), copy.getColumnId());
        }

        for (BlockDatabaseRowVO row : pageCopyMapper.selectRowList(source.getDatabaseId())) {
            BlockDatabaseRowVO copy = new BlockDatabaseRowVO();
            copy.setDatabaseId(database.getDatabaseId());
            copy.setPageId(row.getPageId() == null ? null : pageIdMap.getOrDefault(row.getPageId(), row.getPageId()));
            copy.setPosition(row.getPosition());

            pageCopyMapper.insertRow(copy);

            if (!columnIdMap.isEmpty()) {
                pageCopyMapper.insertCellCopies(row.getRowId(), copy.getRowId(), columnIdMap);
            }
        }
    }

    // 하위 페이지 링크 블록({"pageId": N})의 페이지 번호를 복제본으로 교체
    private String remapPageId(String content, Map<Long, Long> pageIdMap) {

        if (content == null || content.isBlank()) {
            return content;
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(content);

            if (node instanceof ObjectNode object && object.hasNonNull("pageId")
                && object.get("pageId").canConvertToLong()) {

                Long copyId = pageIdMap.get(object.get("pageId").asLong());

                if (copyId != null) {
                    object.put("pageId", copyId);
                    return OBJECT_MAPPER.writeValueAsString(object);
                }
            }

            return content;

        } catch (JsonProcessingException e) {
            return content;
        }
    }

    // 파일 복사 후 새 파일 번호 (원본이 없으면 null)
    private Long copyFile(Long fileId, Long workspaceId, Long userId) {

        if (fileId == null) {
            return null;
        }

        FileVO copy = fileService.copy(fileId, workspaceId, userId);

        return copy == null ? null : copy.getFileId();
    }

    // 페이지 정보 + 블록(댓글 포함) + 하위 페이지 조회
    @Transactional(readOnly = true)
    @Override
    public PageDetailResponse getPageDetail(Long pageId, String email) {

        PageVO page = findActivePage(pageId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        List<BlockResponse> blocks = blockService.getBlocks(pageId, email);

        Map<Long, List<CommentResponse>> commentsByBlock =
            commentService.getBlockCommentMap(blocks.stream().map(BlockResponse::blockId).toList());

        List<PageDetailResponse.BlockItem> blockItems = blocks.stream()
            .map(block -> PageDetailResponse.BlockItem.from(block,
                commentsByBlock.getOrDefault(block.blockId(), List.of())))
            .toList();

        return PageDetailResponse.from(page, blockItems, pageMapper.selectChildPageList(pageId));
    }

    // 페이지 커버 업로드 (기존 커버 삭제 후 저장)
    @Override
    public PageResponse uploadCover(Long pageId, MultipartFile file, String email) {

        PageVO page = findActivePage(pageId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        if (page.getCoverFileId() != null) {
            fileService.delete(page.getCoverFileId());
        }

        FileVO cover = fileService.uploadImage(file, page.getWorkspaceId(), email);

        pageMapper.updatePageCover(pageId, cover.getFileId());

        return toResponse(pageId);
    }

    // 페이지 커버 삭제
    @Override
    public PageResponse deleteCover(Long pageId, String email) {

        PageVO page = findActivePage(pageId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        if (page.getCoverFileId() != null) {
            fileService.delete(page.getCoverFileId());
            pageMapper.updatePageCover(pageId, null);
        }

        return toResponse(pageId);
    }

    // 기본 커버 적용
    @Override
    public PageResponse applyDefaultCover(Long pageId, PageCoverUpdateRequest request, String email) {

        PageVO page = findActivePage(pageId);
        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        if (page.getCoverFileId() != null) {
            fileService.delete(page.getCoverFileId());
        }

        FileVO cover = fileService.createDefaultCover(request.coverName(), page.getWorkspaceId(), findUserId(email));

        pageMapper.updatePageCover(pageId, cover.getFileId());

        return toResponse(pageId);
    }

    // ---------- 조회 + 없으면 예외 ----------

    // 페이지 한 개 (휴지통 포함, 없으면 예외)
    private PageVO findPage(Long pageId) {

        PageVO page = pageMapper.selectPageById(pageId);

        if (page == null) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }

        return page;
    }

    // 휴지통에 없는 페이지 (휴지통이면 PAGE_NOT_FOUND)
    private PageVO findActivePage(Long pageId) {

        PageVO page = findPage(pageId);

        if (Boolean.TRUE.equals(page.getIsDeleted())) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }

        return page;
    }

    private Long findUserId(String email) {

        Long userId = pageMapper.selectUserIdByEmail(email);

        if (userId == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        return userId;
    }

    // 관리자 이상 확인
    private void requireAdmin(Long workspaceId, String email) {

        WorkspaceRole role = workspaceService.getWorkspace(workspaceId, email).role();

        if (!role.isAtLeast(WorkspaceRole.ADMIN)) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }
    }

    // 순환 참조 검증 (새 상위 페이지에서 올라가며 자기 자신 확인)
    private void validateParentHierarchy(Long pageId, Long parentId) {

        Long current = parentId;

        while (current != null) {

            if (current.equals(pageId)) {
                throw new FlowSpaceException(ErrorCode.INVALID_PAGE_PARENT);
            }

            current = pageMapper.selectParentPageId(current);
        }
    }

    // 변경 후 다시 조회해 응답 생성 (수정 시각, 커버 주소 반영)
    private PageResponse toResponse(Long pageId) {
        return PageResponse.from(findPage(pageId));
    }
}
