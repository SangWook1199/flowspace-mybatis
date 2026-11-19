-- 페이지의 블록 목록 (이미지 주소·데이터베이스 번호·페이지 정보 포함)
--   p_page_id  페이지 번호
-- 결과: 블록당 한 행, position 순

DROP PROCEDURE IF EXISTS sp_block_list;

DELIMITER //

CREATE PROCEDURE sp_block_list(IN p_page_id BIGINT)
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
     WHERE b.page_id = p_page_id
     ORDER BY b.position, b.block_id;
END //

DELIMITER ;
