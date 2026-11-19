package com.flowspace.block.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.block.service.BlockVO;
import com.flowspace.entity.enums.BlockType;
import com.flowspace.page.service.PageVO;

// 블록 Mapper (Block_SQL.xml)
@Mapper
public interface BlockMapper {

    // 이메일로 사용자 번호 (없으면 null)
    Long selectUserIdByEmail(String email);

    // ---------- 블록이 있는 페이지 ----------

    // 페이지의 워크스페이스·휴지통 여부·버전 (없으면 null)
    PageVO selectBlockPage(Long pageId);

    // 페이지 정보 + 트랜잭션 종료까지 행 잠금 (휴지통 페이지는 null)
    PageVO selectBlockPageForUpdate(Long pageId);

    // 페이지 버전 +1 (오래된 내용으로 덮어쓰기 방지)
    int updatePageVersion(Long pageId);

    // ---------- 블록 조회 ----------

    // 페이지의 블록 목록 (프로시저 sp_block_list)
    List<BlockVO> selectBlockList(Long pageId);

    // 블록 한 개 (프로시저 sp_block, 없으면 null)
    BlockVO selectBlockById(Long blockId);

    // 페이지의 블록 수 (새 블록을 맨 뒤에 둘 때)
    int selectBlockCount(Long pageId);

    // 여러 블록의 페이지 번호 (순서 바꾸기 확인용)
    List<BlockVO> selectBlockListByIds(@Param("blockIds") List<Long> blockIds);

    // 상위 블록 번호 (최상위면 null)
    Long selectParentBlockId(Long blockId);

    // ---------- 업무 블록이 가리키는 대상의 워크스페이스 (없으면 null) ----------

    Long selectTaskWorkspaceId(Long taskId);

    Long selectEventWorkspaceId(Long eventId);

    Long selectSprintWorkspaceId(Long sprintId);

    // ---------- 등록·수정·삭제 ----------

    // 블록 등록 (생성된 번호는 blockId에 설정)
    int insertBlock(BlockVO block);

    // 종류·내용 수정
    int updateBlockContent(@Param("blockId") Long blockId, @Param("type") BlockType type,
        @Param("content") String content, @Param("updatedBy") Long updatedBy);

    // 일괄 저장에서 내용이 바뀐 블록: 종류·내용·연결·상위 블록·순서를 한 번에 수정
    int updateBlockSync(BlockVO block);

    // 상위 블록과 순서 수정
    int updateBlockParent(@Param("blockId") Long blockId, @Param("parentBlockId") Long parentBlockId,
        @Param("position") BigDecimal position);

    // 순서 수정
    int updateBlockPosition(@Param("blockId") Long blockId, @Param("position") BigDecimal position);

    // 이미지 수정 (fileId가 null이면 이미지 없음)
    int updateBlockImage(@Param("blockId") Long blockId, @Param("fileId") Long fileId);

    // 여러 블록의 상위 블록 연결 끊기
    int updateBlockParentsCleared(@Param("blockIds") List<Long> blockIds);

    // 블록 삭제 (하위 블록·댓글·데이터베이스는 DB에서 연쇄 삭제)
    int deleteBlock(Long blockId);

    // 여러 블록 삭제
    int deleteBlocks(@Param("blockIds") List<Long> blockIds);
}
