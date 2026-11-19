package com.flowspace.database.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.block.service.BlockVO;
import com.flowspace.database.dto.request.BlockDatabaseCellUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnCreateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionCreateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionOrderItem;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionReorderRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnOrderItem;
import com.flowspace.database.dto.request.BlockDatabaseColumnReorderRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnWidthUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseRowCreateRequest;
import com.flowspace.database.dto.request.BlockDatabaseRowOrderItem;
import com.flowspace.database.dto.request.BlockDatabaseRowReorderRequest;
import com.flowspace.database.dto.request.DatabaseCreateRequest;
import com.flowspace.database.dto.request.DatabaseUpdateRequest;
import com.flowspace.database.dto.request.DatabaseViewUpdateRequest;
import com.flowspace.database.dto.response.BlockDatabaseCellResponse;
import com.flowspace.database.dto.response.BlockDatabaseColumnOptionResponse;
import com.flowspace.database.dto.response.BlockDatabaseColumnResponse;
import com.flowspace.database.dto.response.BlockDatabaseRowResponse;
import com.flowspace.database.dto.response.DatabaseDetailResponse;
import com.flowspace.database.dto.response.DatabaseResponse;
import com.flowspace.database.service.BlockDatabaseCellVO;
import com.flowspace.database.service.BlockDatabaseColumnOptionVO;
import com.flowspace.database.service.BlockDatabaseColumnVO;
import com.flowspace.database.service.BlockDatabaseRowVO;
import com.flowspace.database.service.BlockDatabaseService;
import com.flowspace.database.service.BlockDatabaseVO;
import com.flowspace.common.enums.BlockType;
import com.flowspace.common.enums.DatabaseColumnType;
import com.flowspace.common.enums.DatabaseViewType;
import com.flowspace.common.enums.TaskStatusCategory;
import com.flowspace.common.enums.WorkspaceColor;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.page.service.PageService;
import com.flowspace.page.service.PageVO;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BlockDatabaseServiceImpl implements BlockDatabaseService {

    // 빈 제목 대신 쓰는 기본 제목
    private static final String DEFAULT_TITLE = "제목 없음";

    // 우선순위 기본 옵션 대상 컬럼명
    private static final Pattern PRIORITY_OPTION_PATTERN = Pattern.compile("우선순위|priority",
        Pattern.CASE_INSENSITIVE);

    // HTTP 메서드 기본 옵션 대상 컬럼명
    private static final Pattern METHOD_OPTION_PATTERN = Pattern.compile("method|메서드", Pattern.CASE_INSENSITIVE);

    // 데이터베이스 블록 SQL
    private final BlockDatabaseMapper blockDatabaseMapper;
    private final WorkspaceService workspaceService;

    // 행 페이지 휴지통 이동
    private final PageService pageService;

    // 기본 옵션 (값, 색상, 상태 그룹)
    private record OptionSeed(String value, WorkspaceColor color, TaskStatusCategory statusGroup) {}

    // 데이터베이스 생성 (표: 빈 3x3, 데이터베이스: 이름·생성 일시·사람 컬럼 + 첫 행)
    @Override
    public DatabaseResponse createDatabase(Long pageId, DatabaseCreateRequest request, String email) {

        Long userId = findUserId(email);
        PageVO page = blockDatabaseMapper.selectPageForUpdate(pageId);

        if (page == null) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }

        workspaceService.getWorkspace(page.getWorkspaceId(), email);

        BlockVO block = new BlockVO();
        block.setPageId(pageId);
        block.setType(BlockType.DATABASE);
        block.setPosition(BigDecimal.valueOf(blockDatabaseMapper.selectBlockCount(pageId)));
        block.setCreatedBy(userId);

        blockDatabaseMapper.insertBlock(block);

        BlockDatabaseVO database = new BlockDatabaseVO();
        database.setBlockId(block.getBlockId());
        database.setTitle(titleOrDefault(request.title()));
        database.setViewType(request.viewType() == null ? DatabaseViewType.TABLE : request.viewType());

        blockDatabaseMapper.insertDatabase(database);

        blockDatabaseMapper.updatePageVersion(pageId);

        Long databaseId = database.getDatabaseId();

        if (database.getViewType() == DatabaseViewType.TABLE) {
            for (int i = 0; i < 3; i++) {
                insertColumn(databaseId, "", DatabaseColumnType.TEXT, i);
            }

            for (int i = 0; i < 3; i++) {
                insertRow(databaseId, null, i);
            }
        } else {
            insertColumn(databaseId, "이름", DatabaseColumnType.TITLE, 0);
            insertColumn(databaseId, "생성 일시", DatabaseColumnType.CREATED_TIME, 1);
            insertColumn(databaseId, "사람", DatabaseColumnType.PERSON, 2);

            insertRow(databaseId, resolveRowPageId(request.seedPageId(), page.getWorkspaceId(), pageId, userId), 0);
        }

        return DatabaseResponse.from(findDatabase(databaseId));
    }

    // 데이터베이스 단건 조회
    @Transactional(readOnly = true)
    @Override
    public DatabaseResponse getDatabase(Long databaseId, String email) {

        return DatabaseResponse.from(findAccessibleDatabase(databaseId, email));
    }

    // 데이터베이스 제목 수정
    @Override
    public DatabaseResponse updateDatabase(Long databaseId, DatabaseUpdateRequest request, String email) {

        findAccessibleDatabase(databaseId, email);

        blockDatabaseMapper.updateDatabaseTitle(databaseId, titleOrDefault(request.title()));

        return DatabaseResponse.from(findDatabase(databaseId));
    }

    // 데이터베이스 삭제 (블록은 유지)
    @Override
    public void deleteDatabase(Long databaseId, String email) {

        findAccessibleDatabase(databaseId, email);

        blockDatabaseMapper.deleteDatabase(databaseId);
    }

    // 컬럼 생성 (맨 뒤, 기본 옵션 + 기존 행의 빈 셀)
    @Override
    public BlockDatabaseColumnResponse createColumn(Long databaseId, BlockDatabaseColumnCreateRequest request,
        String email) {

        findAccessibleDatabase(databaseId, email);

        Long columnId = insertColumn(databaseId, request.name(), request.type(),
            blockDatabaseMapper.selectColumnCount(databaseId));

        seedDefaultOptionsIfEmpty(columnId, request.type(), request.name());

        blockDatabaseMapper.insertColumnCells(databaseId, columnId);

        return toColumnResponse(columnId);
    }

    // 컬럼 이름·종류 수정 (옵션이 없으면 기본 옵션 추가)
    @Override
    public BlockDatabaseColumnResponse updateColumn(Long columnId, BlockDatabaseColumnUpdateRequest request,
        String email) {

        findAccessibleColumn(columnId, email);

        blockDatabaseMapper.updateColumn(columnId, request.name(), request.type());

        seedDefaultOptionsIfEmpty(columnId, request.type(), request.name());

        return toColumnResponse(columnId);
    }

    // 컬럼 너비 변경
    @Override
    public BlockDatabaseColumnResponse updateColumnWidth(Long columnId, BlockDatabaseColumnWidthUpdateRequest request,
        String email) {

        findAccessibleColumn(columnId, email);

        blockDatabaseMapper.updateColumnWidth(columnId, request.width());

        return toColumnResponse(columnId);
    }

    // 컬럼 순서 변경 (모두 같은 데이터베이스 소속이어야 함)
    @Override
    public void reorderColumns(Long databaseId, BlockDatabaseColumnReorderRequest request, String email) {

        findAccessibleDatabase(databaseId, email);

        List<Long> columnIds = request.columns().stream().map(BlockDatabaseColumnOrderItem::columnId).toList();

        Map<Long, BlockDatabaseColumnVO> columns = blockDatabaseMapper.selectColumnListByIds(columnIds).stream()
            .collect(Collectors.toMap(BlockDatabaseColumnVO::getColumnId, Function.identity()));

        for (BlockDatabaseColumnOrderItem item : request.columns()) {
            BlockDatabaseColumnVO column = columns.get(item.columnId());

            if (column == null) {
                throw new FlowSpaceException(ErrorCode.DATABASE_COLUMN_NOT_FOUND);
            }

            if (!column.getDatabaseId().equals(databaseId)) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }

            blockDatabaseMapper.updateColumnPosition(item.columnId(), item.position());
        }
    }

    // 컬럼 삭제
    @Override
    public void deleteColumn(Long columnId, String email) {

        findAccessibleColumn(columnId, email);

        blockDatabaseMapper.deleteColumn(columnId);
    }

    // 컬럼 옵션 생성 (맨 뒤, 색상 기본 GRAY)
    @Override
    public BlockDatabaseColumnOptionResponse createOption(Long columnId, BlockDatabaseColumnOptionCreateRequest request,
        String email) {

        findAccessibleColumn(columnId, email);

        BlockDatabaseColumnOptionVO option = new BlockDatabaseColumnOptionVO();
        option.setColumnId(columnId);
        option.setValue(request.value());
        option.setColor(colorOrDefault(request.color()));
        option.setStatusGroup(request.statusGroup());
        option.setPosition(blockDatabaseMapper.selectOptionCount(columnId));

        blockDatabaseMapper.insertOption(option);

        return BlockDatabaseColumnOptionResponse.from(findOption(option.getOptionId()));
    }

    // 컬럼 옵션 수정
    @Override
    public BlockDatabaseColumnOptionResponse updateOption(Long optionId, BlockDatabaseColumnOptionUpdateRequest request,
        String email) {

        BlockDatabaseColumnOptionVO option = findOption(optionId);
        findAccessibleColumn(option.getColumnId(), email);

        blockDatabaseMapper.updateOption(optionId, request.value(), colorOrDefault(request.color()),
            request.statusGroup());

        return BlockDatabaseColumnOptionResponse.from(findOption(optionId));
    }

    // 컬럼 옵션 순서 변경 (모두 같은 컬럼 소속이어야 함)
    @Override
    public void reorderOptions(Long columnId, BlockDatabaseColumnOptionReorderRequest request, String email) {

        findAccessibleColumn(columnId, email);

        List<Long> optionIds = request.options().stream().map(BlockDatabaseColumnOptionOrderItem::optionId).toList();

        Map<Long, BlockDatabaseColumnOptionVO> options = blockDatabaseMapper.selectOptionListByIds(optionIds).stream()
            .collect(Collectors.toMap(BlockDatabaseColumnOptionVO::getOptionId, Function.identity()));

        for (BlockDatabaseColumnOptionOrderItem item : request.options()) {
            BlockDatabaseColumnOptionVO option = options.get(item.optionId());

            if (option == null) {
                throw new FlowSpaceException(ErrorCode.DATABASE_OPTION_NOT_FOUND);
            }

            if (!option.getColumnId().equals(columnId)) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }

            blockDatabaseMapper.updateOptionPosition(item.optionId(), item.position());
        }
    }

    // 컬럼 옵션 삭제
    @Override
    public void deleteOption(Long optionId, String email) {

        BlockDatabaseColumnOptionVO option = findOption(optionId);
        findAccessibleColumn(option.getColumnId(), email);

        blockDatabaseMapper.deleteOption(optionId);
    }

    // 행 생성 (맨 뒤, 표는 페이지 없음, 데이터베이스는 행 페이지 연결)
    @Override
    public BlockDatabaseRowResponse createRow(Long databaseId, BlockDatabaseRowCreateRequest request, String email) {

        Long userId = findUserId(email);
        BlockDatabaseVO database = findAccessibleDatabase(databaseId, email);

        Long rowPageId = database.getViewType() == DatabaseViewType.TABLE ? null
            : resolveRowPageId(request == null ? null : request.pageId(), database.getWorkspaceId(),
                database.getPageId(), userId);

        Long rowId = insertRow(databaseId, rowPageId, blockDatabaseMapper.selectRowCount(databaseId));

        return BlockDatabaseRowResponse.from(findRow(rowId));
    }

    // 행 순서 변경 (모두 같은 데이터베이스 소속이어야 함)
    @Override
    public void reorderRows(Long databaseId, BlockDatabaseRowReorderRequest request, String email) {

        findAccessibleDatabase(databaseId, email);

        List<Long> rowIds = request.rows().stream().map(BlockDatabaseRowOrderItem::rowId).toList();

        Map<Long, BlockDatabaseRowVO> rows = blockDatabaseMapper.selectRowListByIds(rowIds).stream()
            .collect(Collectors.toMap(BlockDatabaseRowVO::getRowId, Function.identity()));

        for (BlockDatabaseRowOrderItem item : request.rows()) {
            BlockDatabaseRowVO row = rows.get(item.rowId());

            if (row == null) {
                throw new FlowSpaceException(ErrorCode.DATABASE_ROW_NOT_FOUND);
            }

            if (!row.getDatabaseId().equals(databaseId)) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }

            blockDatabaseMapper.updateRowPosition(item.rowId(), item.position());
        }
    }

    // 행 삭제 (연결된 페이지는 하위 페이지와 함께 휴지통 이동)
    @Override
    public void deleteRow(Long rowId, String email) {

        BlockDatabaseRowVO row = findRow(rowId);
        findAccessibleDatabase(row.getDatabaseId(), email);

        if (row.getPageId() != null) {
            PageVO rowPage = blockDatabaseMapper.selectPage(row.getPageId());

            if (rowPage != null && !Boolean.TRUE.equals(rowPage.getIsDeleted())) {
                pageService.deletePage(row.getPageId(), email);
            }
        }

        blockDatabaseMapper.deleteRow(rowId);
    }

    // 셀 값 수정 (행·컬럼 모두 같은 데이터베이스 소속이어야 함)
    @Override
    public BlockDatabaseCellResponse updateCell(Long databaseId, BlockDatabaseCellUpdateRequest request, String email) {

        findAccessibleDatabase(databaseId, email);

        BlockDatabaseRowVO row = findRow(request.rowId());
        BlockDatabaseColumnVO column = findColumn(request.columnId());

        if (!row.getDatabaseId().equals(databaseId) || !column.getDatabaseId().equals(databaseId)) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }

        if (blockDatabaseMapper.selectCell(request.rowId(), request.columnId()) == null) {
            throw new FlowSpaceException(ErrorCode.DATABASE_CELL_NOT_FOUND);
        }

        blockDatabaseMapper.updateCellValue(request.rowId(), request.columnId(), request.value());

        return BlockDatabaseCellResponse.from(blockDatabaseMapper.selectCell(request.rowId(), request.columnId()));
    }

    // 데이터베이스 전체 조회 (컬럼·옵션·행·셀을 한 번씩 조회 후 조립)
    @Transactional(readOnly = true)
    @Override
    public DatabaseDetailResponse getDatabaseDetail(Long databaseId, String email) {

        BlockDatabaseVO database = findAccessibleDatabase(databaseId, email);

        Map<Long, List<BlockDatabaseColumnOptionResponse>> optionsByColumn = blockDatabaseMapper
            .selectDatabaseOptionList(databaseId).stream()
            .collect(Collectors.groupingBy(BlockDatabaseColumnOptionVO::getColumnId,
                Collectors.mapping(BlockDatabaseColumnOptionResponse::from, Collectors.toList())));

        List<BlockDatabaseColumnResponse> columns = blockDatabaseMapper.selectColumnList(databaseId).stream()
            .map(column -> BlockDatabaseColumnResponse.from(column,
                optionsByColumn.getOrDefault(column.getColumnId(), List.of())))
            .toList();

        Map<Long, List<DatabaseDetailResponse.CellData>> cellsByRow = blockDatabaseMapper
            .selectDatabaseCellList(databaseId).stream()
            .collect(Collectors.groupingBy(BlockDatabaseCellVO::getRowId,
                Collectors.mapping(cell -> new DatabaseDetailResponse.CellData(cell.getColumnId(), cell.getValue()),
                    Collectors.toList())));

        List<DatabaseDetailResponse.RowData> rows = blockDatabaseMapper.selectRowList(databaseId).stream()
            .map(row -> new DatabaseDetailResponse.RowData(row.getRowId(), row.getPageId(), row.getPosition(),
                cellsByRow.getOrDefault(row.getRowId(), List.of())))
            .toList();

        return new DatabaseDetailResponse(databaseId, database.getTitle(), database.getViewType(), columns, rows);
    }

    // 표 / 데이터베이스 전환
    @Override
    public DatabaseResponse updateViewType(Long databaseId, DatabaseViewUpdateRequest request, String email) {

        findAccessibleDatabase(databaseId, email);

        blockDatabaseMapper.updateDatabaseViewType(databaseId, request.viewType());

        return DatabaseResponse.from(findDatabase(databaseId));
    }

    // ---------- 등록 공통 ----------

    // 컬럼 등록 후 번호 반환
    private Long insertColumn(Long databaseId, String name, DatabaseColumnType type, int position) {

        BlockDatabaseColumnVO column = new BlockDatabaseColumnVO();
        column.setDatabaseId(databaseId);
        column.setName(name);
        column.setType(type);
        column.setPosition(position);

        blockDatabaseMapper.insertColumn(column);

        return column.getColumnId();
    }

    // 행 등록 + 컬럼마다 빈 셀, 행 번호 반환
    private Long insertRow(Long databaseId, Long pageId, int position) {

        BlockDatabaseRowVO row = new BlockDatabaseRowVO();
        row.setDatabaseId(databaseId);
        row.setPageId(pageId);
        row.setPosition(position);

        blockDatabaseMapper.insertRow(row);

        blockDatabaseMapper.insertRowCells(databaseId, row.getRowId());

        return row.getRowId();
    }

    // 행 페이지 (같은 워크스페이스의 미사용 기존 페이지, 없으면 하위 페이지 생성)
    private Long resolveRowPageId(Long pageId, Long workspaceId, Long ownerPageId, Long userId) {

        if (pageId != null) {
            PageVO existing = blockDatabaseMapper.selectPage(pageId);

            if (existing == null || Boolean.TRUE.equals(existing.getIsDeleted())) {
                throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
            }

            if (!existing.getWorkspaceId().equals(workspaceId)) {
                throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
            }

            if (blockDatabaseMapper.selectRowCountByPage(pageId) == 0) {
                return pageId;
            }
        }

        PageVO rowPage = new PageVO();
        rowPage.setWorkspaceId(workspaceId);
        rowPage.setParentPageId(ownerPageId);
        rowPage.setCreatedBy(userId);

        blockDatabaseMapper.insertPage(rowPage);

        return rowPage.getPageId();
    }

    // 기본 옵션 등록 (옵션 없는 컬럼만)
    private void seedDefaultOptionsIfEmpty(Long columnId, DatabaseColumnType type, String name) {

        if (blockDatabaseMapper.selectOptionCount(columnId) > 0) {
            return;
        }

        List<OptionSeed> seeds = defaultOptionSeeds(type, name);

        for (int i = 0; i < seeds.size(); i++) {
            BlockDatabaseColumnOptionVO option = new BlockDatabaseColumnOptionVO();
            option.setColumnId(columnId);
            option.setValue(seeds.get(i).value());
            option.setColor(seeds.get(i).color());
            option.setStatusGroup(seeds.get(i).statusGroup());
            option.setPosition(i);

            blockDatabaseMapper.insertOption(option);
        }
    }

    // 컬럼 종류·이름별 기본 옵션 (상태, 우선순위, HTTP 메서드)
    private List<OptionSeed> defaultOptionSeeds(DatabaseColumnType type, String columnName) {

        if (type == DatabaseColumnType.STATUS) {
            return List.of(new OptionSeed("할 일", WorkspaceColor.GRAY, TaskStatusCategory.TODO),
                new OptionSeed("진행 중", WorkspaceColor.BLUE, TaskStatusCategory.IN_PROGRESS),
                new OptionSeed("완료", WorkspaceColor.GREEN, TaskStatusCategory.DONE));
        }

        if (type != DatabaseColumnType.SELECT && type != DatabaseColumnType.MULTI_SELECT) {
            return List.of();
        }

        String name = columnName == null ? "" : columnName;

        if (PRIORITY_OPTION_PATTERN.matcher(name).find()) {
            return List.of(new OptionSeed("높음", WorkspaceColor.RED, null),
                new OptionSeed("보통", WorkspaceColor.ORANGE, null),
                new OptionSeed("낮음", WorkspaceColor.GREEN, null));
        }

        if (METHOD_OPTION_PATTERN.matcher(name).find()) {
            return List.of(new OptionSeed("GET", WorkspaceColor.BLUE, null),
                new OptionSeed("POST", WorkspaceColor.GREEN, null),
                new OptionSeed("PUT", WorkspaceColor.ORANGE, null),
                new OptionSeed("PATCH", WorkspaceColor.ORANGE, null),
                new OptionSeed("DELETE", WorkspaceColor.RED, null));
        }

        return List.of();
    }

    // 컬럼 응답 (옵션 포함)
    private BlockDatabaseColumnResponse toColumnResponse(Long columnId) {

        List<BlockDatabaseColumnOptionResponse> options = blockDatabaseMapper.selectOptionList(columnId).stream()
            .map(BlockDatabaseColumnOptionResponse::from).toList();

        return BlockDatabaseColumnResponse.from(findColumn(columnId), options);
    }

    // 빈 제목은 기본 제목
    private String titleOrDefault(String title) {

        return title == null || title.isBlank() ? DEFAULT_TITLE : title;
    }

    // 빈 색상은 GRAY
    private WorkspaceColor colorOrDefault(WorkspaceColor color) {

        return color == null ? WorkspaceColor.GRAY : color;
    }

    // ---------- 조회 + 없으면 예외 ----------

    private Long findUserId(String email) {

        Long userId = blockDatabaseMapper.selectUserIdByEmail(email);

        if (userId == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        return userId;
    }

    private BlockDatabaseVO findDatabase(Long databaseId) {

        BlockDatabaseVO database = blockDatabaseMapper.selectDatabase(databaseId);

        if (database == null) {
            throw new FlowSpaceException(ErrorCode.DATABASE_NOT_FOUND);
        }

        return database;
    }

    // 데이터베이스 + 멤버 확인 + 휴지통 페이지 차단
    private BlockDatabaseVO findAccessibleDatabase(Long databaseId, String email) {

        BlockDatabaseVO database = findDatabase(databaseId);

        workspaceService.getWorkspace(database.getWorkspaceId(), email);

        if (Boolean.TRUE.equals(database.getPageDeleted())) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }

        return database;
    }

    private BlockDatabaseColumnVO findColumn(Long columnId) {

        BlockDatabaseColumnVO column = blockDatabaseMapper.selectColumn(columnId);

        if (column == null) {
            throw new FlowSpaceException(ErrorCode.DATABASE_COLUMN_NOT_FOUND);
        }

        return column;
    }

    // 컬럼 + 소속 데이터베이스 접근 확인
    private BlockDatabaseColumnVO findAccessibleColumn(Long columnId, String email) {

        BlockDatabaseColumnVO column = findColumn(columnId);

        findAccessibleDatabase(column.getDatabaseId(), email);

        return column;
    }

    private BlockDatabaseColumnOptionVO findOption(Long optionId) {

        BlockDatabaseColumnOptionVO option = blockDatabaseMapper.selectOption(optionId);

        if (option == null) {
            throw new FlowSpaceException(ErrorCode.DATABASE_OPTION_NOT_FOUND);
        }

        return option;
    }

    private BlockDatabaseRowVO findRow(Long rowId) {

        BlockDatabaseRowVO row = blockDatabaseMapper.selectRow(rowId);

        if (row == null) {
            throw new FlowSpaceException(ErrorCode.DATABASE_ROW_NOT_FOUND);
        }

        return row;
    }
}
