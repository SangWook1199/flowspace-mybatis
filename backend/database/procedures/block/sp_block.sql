-- 블록 한 개 (컬럼은 sp_block_list와 동일)
--   p_block_id  블록 번호
-- 결과: 0행 또는 1행

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
           p.workspace_id,
           p.is_deleted     AS page_deleted
      FROM blocks b
      JOIN pages p                ON p.page_id = b.page_id
      LEFT JOIN files f           ON f.file_id = b.image_file_id
      LEFT JOIN block_databases d ON d.block_id = b.block_id
     WHERE b.block_id = p_block_id;
END //

DELIMITER ;
