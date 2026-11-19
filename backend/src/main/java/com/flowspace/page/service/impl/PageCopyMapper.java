package com.flowspace.page.service.impl;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.block.service.BlockVO;
import com.flowspace.database.service.BlockDatabaseColumnVO;
import com.flowspace.database.service.BlockDatabaseRowVO;
import com.flowspace.database.service.BlockDatabaseVO;

// 페이지 복제 Mapper (PageCopy_SQL.xml)
@Mapper
public interface PageCopyMapper {

    // ---------- 블록 ----------

    // 페이지의 블록 목록 (순서대로)
    List<BlockVO> selectBlockList(Long pageId);

    // 블록 복사본 등록 (blockId 채움)
    int insertBlock(BlockVO block);

    // 상위 블록 연결
    int updateBlockParent(@Param("blockId") Long blockId, @Param("parentBlockId") Long parentBlockId);

    // ---------- 데이터베이스 ----------

    // 블록의 데이터베이스 (없으면 null)
    BlockDatabaseVO selectDatabaseByBlock(Long blockId);

    // 데이터베이스 복사본 등록 (databaseId 채움)
    int insertDatabase(BlockDatabaseVO database);

    // 컬럼 목록 (순서대로)
    List<BlockDatabaseColumnVO> selectColumnList(Long databaseId);

    // 컬럼 복사본 등록 (columnId 채움)
    int insertColumn(BlockDatabaseColumnVO column);

    // 원본 컬럼의 옵션을 새 컬럼으로 복사
    int insertOptionCopies(@Param("sourceColumnId") Long sourceColumnId, @Param("columnId") Long columnId);

    // 행 목록 (순서대로)
    List<BlockDatabaseRowVO> selectRowList(Long databaseId);

    // 행 복사본 등록 (rowId 채움)
    int insertRow(BlockDatabaseRowVO row);

    // 원본 행의 셀을 새 행으로 복사 (컬럼 번호는 columnIdMap으로 교체)
    int insertCellCopies(@Param("sourceRowId") Long sourceRowId, @Param("rowId") Long rowId,
        @Param("columnIdMap") Map<Long, Long> columnIdMap);
}
