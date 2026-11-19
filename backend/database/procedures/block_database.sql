-- 데이터베이스 블록 프로시저 (BlockDatabase_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/block_database.sql"


-- 데이터베이스 블록 등록
--   p_page_id     페이지 번호
--   p_type        블록 종류 (DATABASE)
--   p_position    순서
--   p_created_by  만든 사용자 번호
--   p_block_id    새 번호
-- 결과: 없음 (p_block_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_block_database_block_insert;

DELIMITER //

CREATE PROCEDURE sp_block_database_block_insert(IN p_page_id BIGINT, IN p_type VARCHAR(50),
                                                IN p_position DECIMAL(20,10), IN p_created_by BIGINT,
                                                OUT p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO blocks (page_id, type, position, created_by)
    VALUES (p_page_id, p_type, p_position, p_created_by);

    SET p_block_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 행 페이지 등록 (제목·순서는 DB 기본값)
--   p_workspace_id    워크스페이스 번호
--   p_parent_page_id  데이터베이스가 있는 페이지 번호
--   p_created_by      만든 사용자 번호
--   p_page_id         새 번호
-- 결과: 없음 (p_page_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_block_database_row_page_insert;

DELIMITER //

CREATE PROCEDURE sp_block_database_row_page_insert(IN p_workspace_id BIGINT, IN p_parent_page_id BIGINT,
                                                   IN p_created_by BIGINT, OUT p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO pages (workspace_id, parent_page_id, created_by)
    VALUES (p_workspace_id, p_parent_page_id, p_created_by);

    SET p_page_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 페이지를 쓰는 행 수
--   p_page_id  페이지 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_block_database_row_count_by_page;

DELIMITER //

CREATE PROCEDURE sp_block_database_row_count_by_page(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM block_database_rows
    WHERE page_id = p_page_id;
END //

DELIMITER ;


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


-- 데이터베이스 등록
--   p_block_id     블록 번호
--   p_title        데이터베이스 제목
--   p_view_type    표 / 데이터베이스
--   p_database_id  새 번호
-- 결과: 없음 (p_database_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_block_database_insert;

DELIMITER //

CREATE PROCEDURE sp_block_database_insert(IN p_block_id BIGINT, IN p_title VARCHAR(100),
                                          IN p_view_type VARCHAR(50), OUT p_database_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_databases (block_id, title, view_type)
    VALUES (p_block_id, p_title, p_view_type);

    SET p_database_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 제목 수정
--   p_title        데이터베이스 제목
--   p_database_id  데이터베이스 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_title_update;

DELIMITER //

CREATE PROCEDURE sp_block_database_title_update(IN p_title VARCHAR(100), IN p_database_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE block_databases
    SET title = p_title
    WHERE database_id = p_database_id;
END //

DELIMITER ;


-- 표 / 데이터베이스 전환
--   p_view_type    표 / 데이터베이스
--   p_database_id  데이터베이스 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_view_type_update;

DELIMITER //

CREATE PROCEDURE sp_block_database_view_type_update(IN p_view_type VARCHAR(50), IN p_database_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE block_databases
    SET view_type = p_view_type
    WHERE database_id = p_database_id;
END //

DELIMITER ;


-- 데이터베이스 삭제
--   p_database_id  데이터베이스 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_delete;

DELIMITER //

CREATE PROCEDURE sp_block_database_delete(IN p_database_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM block_databases
    WHERE database_id = p_database_id;
END //

DELIMITER ;


-- 컬럼 목록
--   p_database_id  데이터베이스 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_block_database_column_list;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_list(IN p_database_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT column_id,
           database_id,
           name,
           type,
           position,
           width,
           created_at
    FROM block_database_columns
    WHERE database_id = p_database_id
    ORDER BY position, column_id;
END //

DELIMITER ;


-- 컬럼 한 개
--   p_column_id  컬럼 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_block_database_column;

DELIMITER //

CREATE PROCEDURE sp_block_database_column(IN p_column_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT column_id,
           database_id,
           name,
           type,
           position,
           width,
           created_at
    FROM block_database_columns
    WHERE column_id = p_column_id;
END //

DELIMITER ;


-- 여러 컬럼
--   p_column_ids  컬럼 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_block_database_column_list_by_ids;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_list_by_ids(IN p_column_ids JSON)
    READS SQL DATA
BEGIN
    SELECT column_id,
           database_id,
           name,
           type,
           position,
           width,
           created_at
    FROM block_database_columns
    WHERE column_id IN (SELECT id FROM JSON_TABLE(p_column_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 컬럼 수
--   p_database_id  데이터베이스 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_block_database_column_count;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_count(IN p_database_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM block_database_columns
    WHERE database_id = p_database_id;
END //

DELIMITER ;


-- 컬럼 등록 (너비는 DB 기본값)
--   p_database_id  데이터베이스 번호
--   p_name         컬럼 이름
--   p_type         종류
--   p_position     순서
--   p_column_id    새 번호
-- 결과: 없음 (p_column_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_block_database_column_insert;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_insert(IN p_database_id BIGINT, IN p_name VARCHAR(100),
                                                 IN p_type VARCHAR(50), IN p_position INT,
                                                 OUT p_column_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_database_columns (database_id, name, type, position)
    VALUES (p_database_id, p_name, p_type, p_position);

    SET p_column_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 이름·종류 수정
--   p_name       컬럼 이름
--   p_type       종류
--   p_column_id  컬럼 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_column_update;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_update(IN p_name VARCHAR(100), IN p_type VARCHAR(50),
                                                 IN p_column_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE block_database_columns
    SET name = p_name,
        type = p_type
    WHERE column_id = p_column_id;
END //

DELIMITER ;


-- 너비 수정
--   p_width      너비 (px)
--   p_column_id  컬럼 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_column_width_update;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_width_update(IN p_width INT, IN p_column_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE block_database_columns
    SET width = p_width
    WHERE column_id = p_column_id;
END //

DELIMITER ;


-- 순서 수정
--   p_position   순서
--   p_column_id  컬럼 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_column_position_update;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_position_update(IN p_position INT, IN p_column_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE block_database_columns
    SET position = p_position
    WHERE column_id = p_column_id;
END //

DELIMITER ;


-- 컬럼 삭제
--   p_column_id  컬럼 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_column_delete;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_delete(IN p_column_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM block_database_columns
    WHERE column_id = p_column_id;
END //

DELIMITER ;


-- 컬럼의 옵션 목록
--   p_column_id  컬럼 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_block_database_column_option_list;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_option_list(IN p_column_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT option_id,
           column_id,
           value,
           color,
           status_group,
           position,
           created_at
    FROM block_database_column_options
    WHERE column_id = p_column_id
    ORDER BY position, option_id;
END //

DELIMITER ;


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


-- 옵션 한 개
--   p_option_id  옵션 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_block_database_option;

DELIMITER //

CREATE PROCEDURE sp_block_database_option(IN p_option_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT option_id,
           column_id,
           value,
           color,
           status_group,
           position,
           created_at
    FROM block_database_column_options
    WHERE option_id = p_option_id;
END //

DELIMITER ;


-- 여러 옵션
--   p_option_ids  옵션 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_block_database_option_list_by_ids;

DELIMITER //

CREATE PROCEDURE sp_block_database_option_list_by_ids(IN p_option_ids JSON)
    READS SQL DATA
BEGIN
    SELECT option_id,
           column_id,
           value,
           color,
           status_group,
           position,
           created_at
    FROM block_database_column_options
    WHERE option_id IN (SELECT id FROM JSON_TABLE(p_option_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 컬럼의 옵션 수
--   p_column_id  컬럼 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_block_database_option_count;

DELIMITER //

CREATE PROCEDURE sp_block_database_option_count(IN p_column_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM block_database_column_options
    WHERE column_id = p_column_id;
END //

DELIMITER ;


-- 옵션 등록
--   p_column_id     컬럼 번호
--   p_value         옵션 값
--   p_color         색상
--   p_status_group  상태 그룹 (STATUS 옵션만, 아니면 NULL)
--   p_position      순서
--   p_option_id     새 번호
-- 결과: 없음 (p_option_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_block_database_option_insert;

DELIMITER //

CREATE PROCEDURE sp_block_database_option_insert(IN p_column_id BIGINT, IN p_value VARCHAR(100),
                                                 IN p_color VARCHAR(50), IN p_status_group VARCHAR(50),
                                                 IN p_position INT, OUT p_option_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_database_column_options (column_id, value, color, status_group, position)
    VALUES (p_column_id, p_value, p_color, p_status_group, p_position);

    SET p_option_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 값·색상·상태 그룹 수정
--   p_value         옵션 값
--   p_color         색상
--   p_status_group  상태 그룹 (STATUS 옵션만, 아니면 NULL)
--   p_option_id     옵션 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_option_update;

DELIMITER //

CREATE PROCEDURE sp_block_database_option_update(IN p_value VARCHAR(100), IN p_color VARCHAR(50),
                                                 IN p_status_group VARCHAR(50), IN p_option_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE block_database_column_options
    SET value        = p_value,
        color        = p_color,
        status_group = p_status_group
    WHERE option_id = p_option_id;
END //

DELIMITER ;


-- 순서 수정
--   p_position   순서
--   p_option_id  옵션 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_option_position_update;

DELIMITER //

CREATE PROCEDURE sp_block_database_option_position_update(IN p_position INT, IN p_option_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE block_database_column_options
    SET position = p_position
    WHERE option_id = p_option_id;
END //

DELIMITER ;


-- 옵션 삭제
--   p_option_id  옵션 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_option_delete;

DELIMITER //

CREATE PROCEDURE sp_block_database_option_delete(IN p_option_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM block_database_column_options
    WHERE option_id = p_option_id;
END //

DELIMITER ;


-- 행 목록
--   p_database_id  데이터베이스 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_block_database_row_list;

DELIMITER //

CREATE PROCEDURE sp_block_database_row_list(IN p_database_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT row_id,
           database_id,
           page_id,
           position,
           created_at
    FROM block_database_rows
    WHERE database_id = p_database_id
    ORDER BY position, row_id;
END //

DELIMITER ;


-- 행 한 개
--   p_row_id  행 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_block_database_row;

DELIMITER //

CREATE PROCEDURE sp_block_database_row(IN p_row_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT row_id,
           database_id,
           page_id,
           position,
           created_at
    FROM block_database_rows
    WHERE row_id = p_row_id;
END //

DELIMITER ;


-- 여러 행
--   p_row_ids  행 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_block_database_row_list_by_ids;

DELIMITER //

CREATE PROCEDURE sp_block_database_row_list_by_ids(IN p_row_ids JSON)
    READS SQL DATA
BEGIN
    SELECT row_id,
           database_id,
           page_id,
           position,
           created_at
    FROM block_database_rows
    WHERE row_id IN (SELECT id FROM JSON_TABLE(p_row_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 행 수
--   p_database_id  데이터베이스 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_block_database_row_count;

DELIMITER //

CREATE PROCEDURE sp_block_database_row_count(IN p_database_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM block_database_rows
    WHERE database_id = p_database_id;
END //

DELIMITER ;


-- 행 등록
--   p_database_id  데이터베이스 번호
--   p_page_id      행 페이지 번호 (표면 NULL)
--   p_position     순서
--   p_row_id       새 번호
-- 결과: 없음 (p_row_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_block_database_row_insert;

DELIMITER //

CREATE PROCEDURE sp_block_database_row_insert(IN p_database_id BIGINT, IN p_page_id BIGINT, IN p_position INT,
                                              OUT p_row_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_database_rows (database_id, page_id, position)
    VALUES (p_database_id, p_page_id, p_position);

    SET p_row_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 순서 수정
--   p_position  순서
--   p_row_id    행 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_row_position_update;

DELIMITER //

CREATE PROCEDURE sp_block_database_row_position_update(IN p_position INT, IN p_row_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE block_database_rows
    SET position = p_position
    WHERE row_id = p_row_id;
END //

DELIMITER ;


-- 행 삭제
--   p_row_id  행 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_row_delete;

DELIMITER //

CREATE PROCEDURE sp_block_database_row_delete(IN p_row_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM block_database_rows
    WHERE row_id = p_row_id;
END //

DELIMITER ;


-- 새 컬럼의 빈 셀 (기존 행마다)
--   p_column_id    컬럼 번호
--   p_database_id  데이터베이스 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_column_cell_insert;

DELIMITER //

CREATE PROCEDURE sp_block_database_column_cell_insert(IN p_column_id BIGINT, IN p_database_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_database_cells (row_id, column_id)
    SELECT row_id, p_column_id
    FROM block_database_rows
    WHERE database_id = p_database_id;
END //

DELIMITER ;


-- 새 행의 빈 셀 (컬럼마다)
--   p_row_id       행 번호
--   p_database_id  데이터베이스 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_row_cell_insert;

DELIMITER //

CREATE PROCEDURE sp_block_database_row_cell_insert(IN p_row_id BIGINT, IN p_database_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO block_database_cells (row_id, column_id)
    SELECT p_row_id, column_id
    FROM block_database_columns
    WHERE database_id = p_database_id;
END //

DELIMITER ;


-- 셀 한 개
--   p_row_id     행 번호
--   p_column_id  컬럼 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_block_database_cell;

DELIMITER //

CREATE PROCEDURE sp_block_database_cell(IN p_row_id BIGINT, IN p_column_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT row_id,
           column_id,
           value
    FROM block_database_cells
    WHERE row_id = p_row_id
      AND column_id = p_column_id;
END //

DELIMITER ;


-- 값 수정
--   p_value      셀 값 (NULL이면 비움)
--   p_row_id     행 번호
--   p_column_id  컬럼 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_database_cell_value_update;

DELIMITER //

CREATE PROCEDURE sp_block_database_cell_value_update(IN p_value TEXT, IN p_row_id BIGINT,
                                                     IN p_column_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE block_database_cells
    SET value = p_value
    WHERE row_id = p_row_id
      AND column_id = p_column_id;
END //

DELIMITER ;


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
