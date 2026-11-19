package com.flowspace.page.service.impl;

import com.flowspace.page.service.PageVO;
import com.flowspace.workspace.service.WorkspaceService;
import com.flowspace.page.service.PageService;

import com.flowspace.file.service.FileService;
import com.flowspace.file.service.FileVO;

import com.flowspace.activity.service.ActivityService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

import com.flowspace.comment.dto.response.CommentResponse;
import com.flowspace.comment.service.CommentService;
import com.flowspace.page.dto.request.PageCoverUpdateRequest;
import com.flowspace.page.dto.request.PageCreateRequest;
import com.flowspace.page.dto.response.PageDetailResponse;
import com.flowspace.page.dto.request.PageDuplicateRequest;
import com.flowspace.page.dto.request.PageReorderRequest;
import com.flowspace.page.dto.response.PageResponse;
import com.flowspace.page.dto.request.PageUpdateRequest;
import com.flowspace.entity.Block;
import com.flowspace.entity.BlockDatabase;
import com.flowspace.entity.BlockDatabaseCell;
import com.flowspace.entity.BlockDatabaseColumn;
import com.flowspace.entity.BlockDatabaseColumnOption;
import com.flowspace.entity.BlockDatabaseRow;
import com.flowspace.entity.File;
import com.flowspace.entity.Page;
import com.flowspace.entity.User;
import com.flowspace.entity.WorkspaceMember;
import com.flowspace.entity.Workspace;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.entity.enums.BlockType;
import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.repository.BlockDatabaseCellRepository;
import com.flowspace.repository.BlockDatabaseColumnOptionRepository;
import com.flowspace.repository.BlockDatabaseColumnRepository;
import com.flowspace.repository.BlockDatabaseRepository;
import com.flowspace.repository.BlockDatabaseRowRepository;
import com.flowspace.repository.BlockRepository;
import com.flowspace.repository.PageRepository;
import com.flowspace.repository.RetrospectiveRepository;
import com.flowspace.repository.UserRepository;
import com.flowspace.repository.WorkspaceMemberRepository;
import com.flowspace.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;
import com.flowspace.repository.FileRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class PageServiceImpl implements PageService {

    // ↓ JPA 기능용 (영구 삭제·휴지통 비우기·복제·상세 조회, 해당 기능 전환 후 정리)
    private final PageRepository pageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final FileRepository fileRepository;
    private final BlockDatabaseRowRepository blockDatabaseRowRepository;
    private final BlockDatabaseRepository blockDatabaseRepository;
    private final BlockDatabaseColumnRepository blockDatabaseColumnRepository;
    private final BlockDatabaseColumnOptionRepository blockDatabaseColumnOptionRepository;
    private final BlockDatabaseCellRepository blockDatabaseCellRepository;
    private final RetrospectiveRepository retrospectiveRepository;

    private final ActivityService activityService;
    private final WorkspaceService workspaceService;

    // 페이지 SQL
    private final PageMapper pageMapper;
    private final CommentService commentService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    // 휴지통 페이지 영구 삭제
    @Override
    public void deletePagePermanently(Long pageId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Page page = pageRepository.findByPageIdAndIsDeletedTrue(pageId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(page.getWorkspace(), user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        if (!member.getRole().isAtLeast(WorkspaceRole.ADMIN)) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }

        List<Page> subtree = collectSubtree(page);

        for (Page target : subtree) {
            if (retrospectiveRepository.existsByPage(target)) {
                throw new FlowSpaceException(ErrorCode.RETROSPECTIVE_PAGE_PROTECTED);
            }
        }

        List<Page> ordered = new ArrayList<>(subtree);
        Collections.reverse(ordered);

        for (Page target : ordered) {
            purgePage(target);
        }
    }

    // 휴지통 비우기 (회고 페이지는 남겨둠)
    @Override
    public void emptyTrash(Long workspaceId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.WORKSPACE_NOT_FOUND));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        if (!member.getRole().isAtLeast(WorkspaceRole.ADMIN)) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }

        purgeTrashed(pageRepository.findByWorkspaceAndIsDeletedTrueOrderByDeletedAtDesc(workspace));
    }

    // 휴지통에 들어간 지 retentionDays일이 지난 페이지를 모든 워크스페이스에서 영구 삭제 (스케줄러가 호출)
    // 반환값: 삭제를 시도한 대상 수
    @Override
    public int purgeExpiredTrash(int retentionDays) {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Page> expired = pageRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);

        purgeTrashed(expired);

        return expired.size();
    }

    // 휴지통 페이지 영구 삭제 (회고 페이지와 그 상위 페이지는 유지)
    private void purgeTrashed(List<Page> trashed) {

        Map<Long, Page> trashedById = new HashMap<>();
        for (Page page : trashed) {
            trashedById.put(page.getPageId(), page);
        }

        Set<Long> protectedIds = new HashSet<>();
        for (Page page : trashed) {
            if (!retrospectiveRepository.existsByPage(page)) {
                continue;
            }

            Page current = page;
            while (current != null && trashedById.containsKey(current.getPageId())) {
                protectedIds.add(current.getPageId());
                current = current.getParentPage();
            }
        }

        List<Page> targets = new ArrayList<>(trashed);
        targets.removeIf(page -> protectedIds.contains(page.getPageId()));
        targets.sort((a, b) -> depthOf(b) - depthOf(a));

        for (Page target : targets) {
            purgePage(target);
        }
    }

    // 페이지 한 개를 실제로 삭제 (블록/데이터베이스는 DB 연쇄 삭제, 파일은 직접 정리)
    private void purgePage(Page page) {

        List<File> files = new ArrayList<>();

        if (page.getCoverFile() != null) {
            files.add(page.getCoverFile());
        }

        for (File imageFile : blockRepository.findImageFilesByPage(page)) {
            if (!files.contains(imageFile)) {
                files.add(imageFile);
            }
        }

        blockDatabaseRowRepository.deleteAll(blockDatabaseRowRepository.findByPage(page));

        pageRepository.delete(page);
        pageRepository.flush();

        for (File file : files) {
            fileService.delete(file.getFileId());
        }
    }

    // 페이지 깊이 계산
    private int depthOf(Page page) {

        int depth = 0;
        Page current = page.getParentPage();

        while (current != null) {
            depth++;
            current = current.getParentPage();
        }

        return depth;
    }

    // 삭제 여부와 상관없이 본인과 모든 하위 페이지 (상위가 먼저 오는 순서)
    private List<Page> collectSubtree(Page root) {

        List<Page> result = new ArrayList<>();
        result.add(root);

        for (int i = 0; i < result.size(); i++) {
            result.addAll(pageRepository.findByParentPage(result.get(i)));
        }

        return result;
    }

    // 삭제되지 않은 본인과 하위 페이지 (상위가 먼저 오는 순서)
    private List<Page> collectActiveSubtree(Page root) {

        List<Page> result = new ArrayList<>();
        result.add(root);

        for (int i = 0; i < result.size(); i++) {
            result.addAll(pageRepository.findByParentPageAndIsDeletedFalse(result.get(i)));
        }

        return result;
    }

    // 같은 상위 페이지 아래의 페이지 개수 (새 페이지를 맨 뒤에 두기 위해 사용)
    private int countSiblings(Workspace workspace, Page parent) {

        long count = parent == null ? pageRepository.countByWorkspaceAndParentPageIsNullAndIsDeletedFalse(workspace)
            : pageRepository.countByWorkspaceAndParentPageAndIsDeletedFalse(workspace, parent);

        return (int) count;
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

    // 페이지 복제 (하위 페이지, 블록, 데이터베이스까지 함께 복사)
    @Override
    public PageResponse duplicatePage(Long pageId, PageDuplicateRequest request, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Page source = pageRepository.findByPageIdAndIsDeletedFalse(pageId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND));

        Workspace workspace = source.getWorkspace();

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        List<Page> sources = collectActiveSubtree(source);

        Page newParent = source.getParentPage();

        if (request != null && request.parentPageId() != null) {

            newParent = pageRepository.findByPageIdAndIsDeletedFalse(request.parentPageId())
                .orElseThrow(() -> new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND));

            if (!newParent.getWorkspace().getWorkspaceId().equals(workspace.getWorkspaceId())) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }

            for (Page target : sources) {
                if (target.getPageId().equals(newParent.getPageId())) {
                    throw new FlowSpaceException(ErrorCode.INVALID_PAGE_PARENT);
                }
            }
        }

        Map<Long, Page> copies = new LinkedHashMap<>();

        for (Page target : sources) {

            boolean isRoot = target.getPageId().equals(source.getPageId());

            Page parent = isRoot ? newParent : copies.get(target.getParentPage().getPageId());

            File cover = target.getCoverFile() == null ? null
                : fileEntity(fileService.copy(target.getCoverFile().getFileId(), workspace.getWorkspaceId(),
                    user.getUserId()));

            Page copy = Page.builder().workspace(workspace).parentPage(parent).title(target.getTitle())
                .icon(target.getIcon()).coverFile(cover)
                .position(isRoot ? countSiblings(workspace, parent) : target.getPosition()).createdBy(user).build();

            copies.put(target.getPageId(), pageRepository.save(copy));
        }

        for (Page target : sources) {
            copyBlocks(target, copies.get(target.getPageId()), copies, user);
        }

        Page rootCopy = copies.get(source.getPageId());

        activityService.log(workspace, user, ActivityType.PAGE_CREATED, ActivityTargetType.PAGE,
            rootCopy.getPageId());

        return toResponse(rootCopy);
    }

    // 페이지의 블록 복사
    private void copyBlocks(Page source, Page target, Map<Long, Page> pageCopies, User user) {

        List<Block> blocks = blockRepository.findByPageOrderByPositionAsc(source);

        Map<Long, Block> blockCopies = new HashMap<>();

        for (Block block : blocks) {

            File image = block.getImageFile() == null ? null
                : fileEntity(fileService.copy(block.getImageFile().getFileId(), target.getWorkspace().getWorkspaceId(),
                    user.getUserId()));

            Block copy = Block.builder().page(target).task(block.getTask()).event(block.getEvent())
                .sprint(block.getSprint()).imageFile(image).type(block.getType()).position(block.getPosition())
                .content(remapPageId(block.getContent(), pageCopies)).createdBy(user).build();

            blockRepository.save(copy);

            blockCopies.put(block.getBlockId(), copy);

            if (block.getType() == BlockType.DATABASE) {
                blockDatabaseRepository.findByBlock(block)
                    .ifPresent(database -> copyDatabase(database, copy, pageCopies));
            }
        }

        for (Block block : blocks) {
            if (block.getParentBlock() != null) {
                blockCopies.get(block.getBlockId())
                    .updateParent(blockCopies.get(block.getParentBlock().getBlockId()));
            }
        }
    }

    // 데이터베이스 복사 (컬럼, 옵션, 행, 셀)
    private void copyDatabase(BlockDatabase source, Block newBlock, Map<Long, Page> pageCopies) {

        BlockDatabase database = blockDatabaseRepository.save(
            BlockDatabase.builder().block(newBlock).title(source.getTitle()).viewType(source.getViewType()).build());

        Map<Long, BlockDatabaseColumn> columnCopies = new HashMap<>();

        for (BlockDatabaseColumn column : blockDatabaseColumnRepository.findByDatabaseOrderByPositionAsc(source)) {

            BlockDatabaseColumn columnCopy = blockDatabaseColumnRepository
                .save(BlockDatabaseColumn.builder().database(database).name(column.getName()).type(column.getType())
                    .position(column.getPosition()).width(column.getWidth()).build());

            columnCopies.put(column.getColumnId(), columnCopy);

            for (BlockDatabaseColumnOption option : blockDatabaseColumnOptionRepository
                .findByColumnOrderByPositionAsc(column)) {

                blockDatabaseColumnOptionRepository.save(BlockDatabaseColumnOption.builder().column(columnCopy)
                    .value(option.getValue()).color(option.getColor()).statusGroup(option.getStatusGroup())
                    .position(option.getPosition()).build());
            }
        }

        for (BlockDatabaseRow row : blockDatabaseRowRepository.findByDatabaseOrderByPositionAsc(source)) {

            Page rowPage = row.getPage() == null ? null
                : pageCopies.getOrDefault(row.getPage().getPageId(), row.getPage());

            BlockDatabaseRow rowCopy = blockDatabaseRowRepository.save(BlockDatabaseRow.builder()
                .database(database).page(rowPage).position(row.getPosition()).build());

            for (BlockDatabaseCell cell : blockDatabaseCellRepository.findByRowOrderByColumnPositionAsc(row)) {

                BlockDatabaseColumn columnCopy = columnCopies.get(cell.getColumn().getColumnId());

                if (columnCopy == null) {
                    continue;
                }

                blockDatabaseCellRepository.save(BlockDatabaseCell.builder().row(rowCopy).column(columnCopy)
                    .value(cell.getValue()).build());
            }
        }
    }

    // 하위 페이지 링크 블록({"pageId": N})의 페이지 ID를 복제본으로 교체
    private String remapPageId(String content, Map<Long, Page> pageCopies) {

        if (content == null || content.isBlank()) {
            return content;
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(content);

            if (node instanceof ObjectNode object && object.hasNonNull("pageId")
                && object.get("pageId").canConvertToLong()) {

                Page copy = pageCopies.get(object.get("pageId").asLong());

                if (copy != null) {
                    object.put("pageId", copy.getPageId());
                    return OBJECT_MAPPER.writeValueAsString(object);
                }
            }

            return content;

        } catch (JsonProcessingException e) {
            return content;
        }
    }

    // 페이지 정보와 블록 전체 조회
    @Transactional(readOnly = true)
    @Override
    public PageDetailResponse getPageDetail(Long pageId, String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.USER_NOT_FOUND));

        Page page = pageRepository.findByPageIdAndIsDeletedFalse(pageId)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND));

        workspaceMemberRepository.findByWorkspaceAndUser(page.getWorkspace(), user)
            .orElseThrow(() -> new FlowSpaceException(ErrorCode.ACCESS_DENIED));

        List<Block> blocks = blockRepository.findWithImageAndDatabaseByPageOrderByPositionAsc(page);

        Map<Long, List<CommentResponse>> commentsByBlock =
            commentService.getBlockCommentMap(blocks.stream().map(Block::getBlockId).toList());

        List<PageDetailResponse.BlockItem> blockItems = blocks.stream()
            .map(block -> PageDetailResponse.BlockItem.from(block,
                commentsByBlock.getOrDefault(block.getBlockId(), List.of())))
            .toList();

        List<Page> childPages = pageRepository.findByParentPageAndIsDeletedFalse(page);

        return PageDetailResponse.from(page, blockItems, childPages);
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

    // FileVO를 JPA File 엔티티 참조로 변환 (복제 전환 후 삭제)
    // getReferenceById는 실제 사용 시점에 조회, 원본이 없으면 null
    private File fileEntity(FileVO file) {
        return file == null ? null : fileRepository.getReferenceById(file.getFileId());
    }

    // 페이지 엔티티 응답 변환 (회고 페이지 여부 포함)
    private PageResponse toResponse(Page page) {
        return PageResponse.from(page, retrospectiveRepository.existsByPage(page));
    }

    // ---------- MyBatis로 바꾼 기능의 도우미 ----------

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
