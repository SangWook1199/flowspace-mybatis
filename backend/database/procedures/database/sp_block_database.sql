-- 데이터베이스 블록 한 개 (블록이 있는 페이지·워크스페이스·휴지통 여부 포함)
--   p_database_id  데이터베이스 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_block_database;

DELIMITER //

CREATE PROCEDURE sp_block_database(IN p_database_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT d.database_id,
           d.block_id,
           d.title,
           d.view_type,
           d.created_at,
           b.page_id,
           p.workspace_id,
           p.is_deleted AS page_deleted
      FROM block_databases d
      JOIN blocks b ON b.block_id = d.block_id
      JOIN pages p  ON p.page_id = b.page_id
     WHERE d.database_id = p_database_id;
END //

DELIMITER ;
