package com.flowspace.database.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.block.service.BlockVO;
import com.flowspace.database.service.BlockDatabaseCellVO;
import com.flowspace.database.service.BlockDatabaseColumnOptionVO;
import com.flowspace.database.service.BlockDatabaseColumnVO;
import com.flowspace.database.service.BlockDatabaseRowVO;
import com.flowspace.database.service.BlockDatabaseVO;
import com.flowspace.common.enums.DatabaseColumnType;
import com.flowspace.common.enums.DatabaseViewType;
import com.flowspace.common.enums.TaskStatusCategory;
import com.flowspace.common.enums.WorkspaceColor;
import com.flowspace.page.service.PageVO;

// 데이터베이스 블록 Mapper (BlockDatabase_SQL.xml)
@Mapper
public interface BlockDatabaseMapper {

    // 이메일로 사용자 번호 (없으면 null)
    Long selectUserIdByEmail(String email);

    // ---------- 페이지·블록 ----------

    // 페이지의 워크스페이스·휴지통 여부 (없으면 null)
    PageVO selectPage(Long pageId);

    // 페이지 정보 + 트랜잭션 종료까지 행 잠금 (휴지통 페이지는 null)
    PageVO selectPageForUpdate(Long pageId);

    // 페이지 버전 +1
    int updatePageVersion(Long pageId);

    // 페이지의 블록 수 (새 블록을 맨 뒤에 둘 때)
    int selectBlockCount(Long pageId);

    // 데이터베이스 블록 등록 (blockId 채움)
    int insertBlock(BlockVO block);

    // 행 페이지 등록 (pageId 채움)
    int insertPage(PageVO page);

    // 페이지를 쓰는 행 수
    int selectRowCountByPage(Long pageId);

    // ---------- 데이터베이스 ----------

    // 데이터베이스 한 개 (프로시저 sp_block_database, 없으면 null)
    BlockDatabaseVO selectDatabase(Long databaseId);

    // 데이터베이스 등록 (databaseId 채움)
    int insertDatabase(BlockDatabaseVO database);

    // 제목 수정
    int updateDatabaseTitle(@Param("databaseId") Long databaseId, @Param("title") String title);

    // 표 / 데이터베이스 전환
    int updateDatabaseViewType(@Param("databaseId") Long databaseId, @Param("viewType") DatabaseViewType viewType);

    // 데이터베이스 삭제 (컬럼·행·셀·옵션은 CASCADE)
    int deleteDatabase(Long databaseId);

    // ---------- 컬럼 ----------

    // 컬럼 목록 (순서대로)
    List<BlockDatabaseColumnVO> selectColumnList(Long databaseId);

    // 컬럼 한 개 (없으면 null)
    BlockDatabaseColumnVO selectColumn(Long columnId);

    // 여러 컬럼 (순서 바꾸기 확인용)
    List<BlockDatabaseColumnVO> selectColumnListByIds(@Param("columnIds") List<Long> columnIds);

    // 컬럼 수
    int selectColumnCount(Long databaseId);

    // 컬럼 등록 (columnId 채움)
    int insertColumn(BlockDatabaseColumnVO column);

    // 이름·종류 수정
    int updateColumn(@Param("columnId") Long columnId, @Param("name") String name,
        @Param("type") DatabaseColumnType type);

    // 너비 수정
    int updateColumnWidth(@Param("columnId") Long columnId, @Param("width") Integer width);

    // 순서 수정
    int updateColumnPosition(@Param("columnId") Long columnId, @Param("position") Integer position);

    // 컬럼 삭제 (셀·옵션은 CASCADE)
    int deleteColumn(Long columnId);

    // ---------- 옵션 ----------

    // 컬럼의 옵션 목록 (순서대로)
    List<BlockDatabaseColumnOptionVO> selectOptionList(Long columnId);

    // 데이터베이스의 모든 옵션 (프로시저 sp_block_database_option_list)
    List<BlockDatabaseColumnOptionVO> selectDatabaseOptionList(Long databaseId);

    // 옵션 한 개 (없으면 null)
    BlockDatabaseColumnOptionVO selectOption(Long optionId);

    // 여러 옵션 (순서 바꾸기 확인용)
    List<BlockDatabaseColumnOptionVO> selectOptionListByIds(@Param("optionIds") List<Long> optionIds);

    // 컬럼의 옵션 수
    int selectOptionCount(Long columnId);

    // 옵션 등록 (optionId 채움)
    int insertOption(BlockDatabaseColumnOptionVO option);

    // 값·색상·상태 그룹 수정
    int updateOption(@Param("optionId") Long optionId, @Param("value") String value,
        @Param("color") WorkspaceColor color, @Param("statusGroup") TaskStatusCategory statusGroup);

    // 순서 수정
    int updateOptionPosition(@Param("optionId") Long optionId, @Param("position") Integer position);

    // 옵션 삭제
    int deleteOption(Long optionId);

    // ---------- 행 ----------

    // 행 목록 (순서대로)
    List<BlockDatabaseRowVO> selectRowList(Long databaseId);

    // 행 한 개 (없으면 null)
    BlockDatabaseRowVO selectRow(Long rowId);

    // 여러 행 (순서 바꾸기 확인용)
    List<BlockDatabaseRowVO> selectRowListByIds(@Param("rowIds") List<Long> rowIds);

    // 행 수
    int selectRowCount(Long databaseId);

    // 행 등록 (rowId 채움)
    int insertRow(BlockDatabaseRowVO row);

    // 순서 수정
    int updateRowPosition(@Param("rowId") Long rowId, @Param("position") Integer position);

    // 행 삭제 (셀은 CASCADE)
    int deleteRow(Long rowId);

    // ---------- 셀 ----------

    // 새 컬럼의 빈 셀 (기존 행마다 하나)
    int insertColumnCells(@Param("databaseId") Long databaseId, @Param("columnId") Long columnId);

    // 새 행의 빈 셀 (컬럼마다 하나)
    int insertRowCells(@Param("databaseId") Long databaseId, @Param("rowId") Long rowId);

    // 셀 한 개 (없으면 null)
    BlockDatabaseCellVO selectCell(@Param("rowId") Long rowId, @Param("columnId") Long columnId);

    // 값 수정
    int updateCellValue(@Param("rowId") Long rowId, @Param("columnId") Long columnId, @Param("value") String value);

    // 데이터베이스의 모든 셀 (프로시저 sp_block_database_cell_list)
    List<BlockDatabaseCellVO> selectDatabaseCellList(Long databaseId);
}
