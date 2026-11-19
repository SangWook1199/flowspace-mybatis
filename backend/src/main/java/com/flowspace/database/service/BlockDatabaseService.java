package com.flowspace.database.service;

import com.flowspace.database.dto.request.BlockDatabaseCellUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnCreateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionCreateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionReorderRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnReorderRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnWidthUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseRowCreateRequest;
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

// 데이터베이스 블록 서비스
public interface BlockDatabaseService {

    // 데이터베이스 생성
    DatabaseResponse createDatabase(Long pageId, DatabaseCreateRequest request, String email);

    // 데이터베이스 단건 조회
    DatabaseResponse getDatabase(Long databaseId, String email);

    // 데이터베이스 제목 수정
    DatabaseResponse updateDatabase(Long databaseId, DatabaseUpdateRequest request, String email);

    // 데이터베이스 삭제
    void deleteDatabase(Long databaseId, String email);

    // 컬럼 생성
    BlockDatabaseColumnResponse createColumn(Long databaseId, BlockDatabaseColumnCreateRequest request, String email);

    // 컬럼 수정
    BlockDatabaseColumnResponse updateColumn(Long columnId, BlockDatabaseColumnUpdateRequest request, String email);

    // 컬럼 너비 변경
    BlockDatabaseColumnResponse updateColumnWidth(Long columnId, BlockDatabaseColumnWidthUpdateRequest request,
        String email);

    // 컬럼 순서 변경
    void reorderColumns(Long databaseId, BlockDatabaseColumnReorderRequest request, String email);

    // 컬럼 삭제
    void deleteColumn(Long columnId, String email);

    // 컬럼 옵션 생성
    BlockDatabaseColumnOptionResponse createOption(Long columnId, BlockDatabaseColumnOptionCreateRequest request,
        String email);

    // 컬럼 옵션 수정
    BlockDatabaseColumnOptionResponse updateOption(Long optionId, BlockDatabaseColumnOptionUpdateRequest request,
        String email);

    // 컬럼 옵션 순서 변경
    void reorderOptions(Long columnId, BlockDatabaseColumnOptionReorderRequest request, String email);

    // 컬럼 옵션 삭제
    void deleteOption(Long optionId, String email);

    // 행 생성
    BlockDatabaseRowResponse createRow(Long databaseId, BlockDatabaseRowCreateRequest request, String email);

    // 행 순서 변경
    void reorderRows(Long databaseId, BlockDatabaseRowReorderRequest request, String email);

    // 행 삭제
    void deleteRow(Long rowId, String email);

    // 셀 수정
    BlockDatabaseCellResponse updateCell(Long databaseId, BlockDatabaseCellUpdateRequest request, String email);

    // 데이터베이스 전체 조회
    DatabaseDetailResponse getDatabaseDetail(Long databaseId, String email);

    // 표 / 데이터베이스 전환
    DatabaseResponse updateViewType(Long databaseId, DatabaseViewUpdateRequest request, String email);
}
