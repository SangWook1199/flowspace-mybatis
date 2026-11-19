-- 블록 한 개를 조회해요. 컬럼은 sp_block_list와 같아요(함께 고쳐요).
--   인자: p_block_id  블록 번호
--   결과: 한 행 (없으면 0행)

DROP PROCEDURE IF EXISTS sp_block;

DELIMITER //

CREATE PROCEDURE sp_block(IN p_block_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT b.block_id,
           b.page_id,
           b.parent_block_id,
           b.task_id,
           b.event_id,
           b.sprint_id,
           b.image_file_id,
           f.file_url       AS image_url,
           d.database_id,
           b.type,
           b.position,
           b.content,
           b.created_by,
           b.updated_by,
           b.created_at,
           b.updated_at,
           -- 블록이 있는 페이지 (멤버 확인, 휴지통 확인에 써요)
           p.workspace_id,
           p.is_deleted     AS page_deleted
      FROM blocks b
      JOIN pages p                ON p.page_id = b.page_id
      LEFT JOIN files f           ON f.file_id = b.image_file_id
      LEFT JOIN block_databases d ON d.block_id = b.block_id
     WHERE b.block_id = p_block_id;
END //

DELIMITER ;
