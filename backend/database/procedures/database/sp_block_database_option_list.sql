-- 데이터베이스의 모든 컬럼 옵션
--   p_database_id  데이터베이스 번호
-- 결과: 옵션당 한 행, column_id → position 순

DROP PROCEDURE IF EXISTS sp_block_database_option_list;

DELIMITER //

CREATE PROCEDURE sp_block_database_option_list(IN p_database_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT o.option_id,
           o.column_id,
           o.value,
           o.color,
           o.status_group,
           o.position,
           o.created_at
      FROM block_database_column_options o
      JOIN block_database_columns c ON c.column_id = o.column_id
     WHERE c.database_id = p_database_id
     ORDER BY o.column_id, o.position, o.option_id;
END //

DELIMITER ;
