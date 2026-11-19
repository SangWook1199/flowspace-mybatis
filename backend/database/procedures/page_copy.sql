-- 페이지 복제 프로시저 (PageCopy_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/page_copy.sql"


-- 페이지의 블록 목록
--   p_page_id  페이지 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_copy_block_list;

DELIMITER //

CREATE PROCEDURE sp_copy_block_list(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT block_id,
           parent_block_id,
           task_id,
           event_id,
           sprint_id,
           image_file_id,
           type,
           position,
           content
    FROM blocks
    WHERE page_id = p_page_id
    ORDER BY position, block_id;
END //

DELIMITER ;


-- 블록 복사본 등록 (상위 블록은 나중에 연결)
--   p_page_id        페이지 번호
--   p_task_id        연결 작업 번호 (NULL이면 없음)
--   p_event_id       연결 일정 번호 (NULL이면 없음)
--   p_sprint_id      연결 스프린트 번호 (NULL이면 없음)
--   p_image_file_id  이미지 파일 번호 (NULL이면 없음)
--   p_type           종류
--   p_position       순서
--   p_content        블록 내용 (JSON)
--   p_created_by     만든 사용자 번호
--   p_block_id       새 번호
-- 결과: 없음 (p_block_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_copy_block_insert;

DELIMITER //

CREATE PROCEDURE sp_copy_block_insert(IN p_page_id BIGINT, IN p_task_id BIGINT, IN p_event_id BIGINT,
                                      IN p_sprint_id BIGINT, IN p_image_file_id BIGINT, IN p_type VARCHAR(50),
                                      IN p_position DECIMAL(20,10), IN p_content JSON, IN p_created_by BIGINT,
                                      OUT p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO blocks (page_id, task_id, event_id, sprint_id, image_file_id, type, position, content,
                        created_by)
    VALUES (p_page_id, p_task_id, p_event_id, p_sprint_id, p_image_file_id, p_type, p_position, p_content,
            p_created_by);

    SET p_block_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 상위 블록 연결
--   p_parent_block_id  상위 블록 번호
--   p_block_id         블록 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_copy_block_parent_update;

DELIMITER //

CREATE PROCEDURE sp_copy_block_parent_update(IN p_parent_block_id BIGINT, IN p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE blocks
    SET parent_block_id = p_parent_block_id
    WHERE block_id = p_block_id;
END //

DELIMITER ;


-- 블록의 데이터베이스
--   p_block_id  블록 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_copy_database;

DELIMITER //

CREATE PROCEDURE sp_copy_database(IN p_block_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT database_id,
           block_id,
           title,
           view_type
    FROM block_databases
    WHERE block_id = p_block_id;
END //

DELIMITER ;


-- 데이터베이스 복사본 등록
--   p_block_id     블록 번호
--   p_title        제목
--   p_view_type    표 / 데이터베이스
--   p_database_id  새 번호
-- 결과: 없음 (p_database_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_copy_database_insert;

DELIMITER //

CREATE PROCEDURE sp_copy_database_insert(IN p_block_id BIGINT, IN p_title VARCHAR(100),
                                         IN p_view_type VARCHAR(50), OUT p_database_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_databases (block_id, title, view_type)
    VALUES (p_block_id, p_title, p_view_type);

    SET p_database_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 컬럼 목록
--   p_database_id  데이터베이스 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_copy_column_list;

DELIMITER //

CREATE PROCEDURE sp_copy_column_list(IN p_database_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT column_id,
           database_id,
           name,
           type,
           position,
           width
    FROM block_database_columns
    WHERE database_id = p_database_id
    ORDER BY position, column_id;
END //

DELIMITER ;


-- 컬럼 복사본 등록
--   p_database_id  데이터베이스 번호
--   p_name         이름
--   p_type         종류
--   p_position     순서
--   p_width        너비
--   p_column_id    새 번호
-- 결과: 없음 (p_column_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_copy_column_insert;

DELIMITER //

CREATE PROCEDURE sp_copy_column_insert(IN p_database_id BIGINT, IN p_name VARCHAR(100), IN p_type VARCHAR(50),
                                       IN p_position INT, IN p_width INT, OUT p_column_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_database_columns (database_id, name, type, position, width)
    VALUES (p_database_id, p_name, p_type, p_position, p_width);

    SET p_column_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 옵션 복사 (원본 컬럼 → 새 컬럼)
--   p_column_id         새 컬럼 번호
--   p_source_column_id  원본 컬럼 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_copy_option_insert;

DELIMITER //

CREATE PROCEDURE sp_copy_option_insert(IN p_column_id BIGINT, IN p_source_column_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_database_column_options (column_id, value, color, status_group, position)
    SELECT p_column_id, value, color, status_group, position
    FROM block_database_column_options
    WHERE column_id = p_source_column_id;
END //

DELIMITER ;


-- 행 목록
--   p_database_id  데이터베이스 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_copy_row_list;

DELIMITER //

CREATE PROCEDURE sp_copy_row_list(IN p_database_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT row_id,
           database_id,
           page_id,
           position
    FROM block_database_rows
    WHERE database_id = p_database_id
    ORDER BY position, row_id;
END //

DELIMITER ;


-- 행 복사본 등록
--   p_database_id  데이터베이스 번호
--   p_page_id      행 페이지 번호 (표면 NULL)
--   p_position     순서
--   p_row_id       새 번호
-- 결과: 없음 (p_row_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_copy_row_insert;

DELIMITER //

CREATE PROCEDURE sp_copy_row_insert(IN p_database_id BIGINT, IN p_page_id BIGINT, IN p_position INT,
                                    OUT p_row_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_database_rows (database_id, page_id, position)
    VALUES (p_database_id, p_page_id, p_position);

    SET p_row_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 셀 복사 (원본 행 → 새 행, 컬럼 번호는 원본 → 새 번호 쌍으로 교체)
--   p_source_row_id  원본 행 번호
--   p_row_id         새 행 번호
--   p_column_id_map  원본 → 새 컬럼 번호 쌍 ([[원본, 새], ...])
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_copy_cell_insert;

DELIMITER //

CREATE PROCEDURE sp_copy_cell_insert(IN p_source_row_id BIGINT, IN p_row_id BIGINT, IN p_column_id_map JSON)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_database_cells (row_id, column_id, value)
    SELECT p_row_id, m.column_id, c.value
      FROM block_database_cells c
      JOIN JSON_TABLE(p_column_id_map, '$[*]'
                      COLUMNS (source_id BIGINT PATH '$[0]', column_id BIGINT PATH '$[1]')) AS m
        ON m.source_id = c.column_id
     WHERE c.row_id = p_source_row_id;
END //

DELIMITER ;
