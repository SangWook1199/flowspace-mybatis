-- 데이터베이스의 모든 셀
--   p_database_id  데이터베이스 번호
-- 결과: 셀당 한 행, row_id → 컬럼 position 순

DROP PROCEDURE IF EXISTS sp_block_database_cell_list;

DELIMITER //

CREATE PROCEDURE sp_block_database_cell_list(IN p_database_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT ce.row_id,
           ce.column_id,
           ce.value
      FROM block_database_cells ce
      JOIN block_database_columns c ON c.column_id = ce.column_id
     WHERE c.database_id = p_database_id
     ORDER BY ce.row_id, c.position, c.column_id;
END //

DELIMITER ;
