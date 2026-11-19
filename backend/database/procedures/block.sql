-- 블록 프로시저 (Block_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/block.sql"


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


-- 페이지의 블록 수
--   p_page_id  페이지 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_page_block_count;

DELIMITER //

CREATE PROCEDURE sp_page_block_count(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM blocks
    WHERE page_id = p_page_id;
END //

DELIMITER ;


-- 여러 블록의 페이지 번호
--   p_block_ids  블록 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_block_list_by_ids;

DELIMITER //

CREATE PROCEDURE sp_block_list_by_ids(IN p_block_ids JSON)
    READS SQL DATA
BEGIN
    SELECT block_id,
           page_id
    FROM blocks
    WHERE block_id IN (SELECT id FROM JSON_TABLE(p_block_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 상위 블록 번호
--   p_block_id  블록 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_block_parent_id;

DELIMITER //

CREATE PROCEDURE sp_block_parent_id(IN p_block_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT parent_block_id
    FROM blocks
    WHERE block_id = p_block_id;
END //

DELIMITER ;


-- 작업의 워크스페이스
--   p_task_id  작업 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_task_workspace_id;

DELIMITER //

CREATE PROCEDURE sp_task_workspace_id(IN p_task_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT workspace_id
    FROM tasks
    WHERE task_id = p_task_id;
END //

DELIMITER ;


-- 일정의 워크스페이스
--   p_event_id  일정 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_event_workspace_id;

DELIMITER //

CREATE PROCEDURE sp_event_workspace_id(IN p_event_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT workspace_id
    FROM events
    WHERE event_id = p_event_id;
END //

DELIMITER ;


-- 블록 등록 (content는 JSON 컬럼)
--   p_page_id          페이지 번호
--   p_parent_block_id  상위 블록 번호 (NULL이면 최상위)
--   p_task_id          연결 작업 번호 (NULL이면 없음)
--   p_event_id         연결 일정 번호 (NULL이면 없음)
--   p_sprint_id        연결 스프린트 번호 (NULL이면 없음)
--   p_type             종류
--   p_position         순서
--   p_content          블록 내용 (JSON)
--   p_created_by       만든 사용자 번호
--   p_block_id         새 번호
-- 결과: 없음 (p_block_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_block_insert;

DELIMITER //

CREATE PROCEDURE sp_block_insert(IN p_page_id BIGINT, IN p_parent_block_id BIGINT, IN p_task_id BIGINT,
                                 IN p_event_id BIGINT, IN p_sprint_id BIGINT, IN p_type VARCHAR(50),
                                 IN p_position DECIMAL(20,10), IN p_content JSON, IN p_created_by BIGINT,
                                 OUT p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO blocks (page_id, parent_block_id, task_id, event_id, sprint_id, type, position, content,
                        created_by)
    VALUES (p_page_id, p_parent_block_id, p_task_id, p_event_id, p_sprint_id, p_type, p_position, p_content,
            p_created_by);

    SET p_block_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 종류·내용 수정
--   p_type        종류
--   p_content     블록 내용 (JSON)
--   p_updated_by  수정한 사용자 번호
--   p_block_id    블록 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_content_update;

DELIMITER //

CREATE PROCEDURE sp_block_content_update(IN p_type VARCHAR(50), IN p_content JSON, IN p_updated_by BIGINT,
                                         IN p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE blocks
    SET type       = p_type,
        content    = p_content,
        updated_by = p_updated_by
    WHERE block_id = p_block_id;
END //

DELIMITER ;


-- 일괄 저장: 종류·내용·연결·상위 블록·순서
--   p_type             종류
--   p_content          블록 내용 (JSON)
--   p_task_id          연결 작업 번호 (NULL이면 없음)
--   p_event_id         연결 일정 번호 (NULL이면 없음)
--   p_sprint_id        연결 스프린트 번호 (NULL이면 없음)
--   p_parent_block_id  상위 블록 번호 (NULL이면 최상위)
--   p_position         순서
--   p_updated_by       수정한 사용자 번호
--   p_block_id         블록 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_sync_update;

DELIMITER //

CREATE PROCEDURE sp_block_sync_update(IN p_type VARCHAR(50), IN p_content JSON, IN p_task_id BIGINT,
                                      IN p_event_id BIGINT, IN p_sprint_id BIGINT, IN p_parent_block_id BIGINT,
                                      IN p_position DECIMAL(20,10), IN p_updated_by BIGINT,
                                      IN p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE blocks
    SET type            = p_type,
        content         = p_content,
        task_id         = p_task_id,
        event_id        = p_event_id,
        sprint_id       = p_sprint_id,
        parent_block_id = p_parent_block_id,
        position        = p_position,
        updated_by      = p_updated_by
    WHERE block_id = p_block_id;
END //

DELIMITER ;


-- 상위 블록과 순서
--   p_parent_block_id  상위 블록 번호 (NULL이면 최상위)
--   p_position         순서
--   p_block_id         블록 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_parent_update;

DELIMITER //

CREATE PROCEDURE sp_block_parent_update(IN p_parent_block_id BIGINT, IN p_position DECIMAL(20,10),
                                        IN p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE blocks
    SET parent_block_id = p_parent_block_id,
        position        = p_position
    WHERE block_id = p_block_id;
END //

DELIMITER ;


-- 순서
--   p_position  순서
--   p_block_id  블록 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_position_update;

DELIMITER //

CREATE PROCEDURE sp_block_position_update(IN p_position DECIMAL(20,10), IN p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE blocks
    SET position = p_position
    WHERE block_id = p_block_id;
END //

DELIMITER ;


-- 이미지
--   p_file_id   이미지 파일 번호 (NULL이면 없음)
--   p_block_id  블록 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_image_update;

DELIMITER //

CREATE PROCEDURE sp_block_image_update(IN p_file_id BIGINT, IN p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE blocks
    SET image_file_id = p_file_id
    WHERE block_id = p_block_id;
END //

DELIMITER ;


-- 여러 블록의 상위 블록 연결 끊기
--   p_block_ids  블록 번호 목록 (JSON 배열)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_parent_clear;

DELIMITER //

CREATE PROCEDURE sp_block_parent_clear(IN p_block_ids JSON)
    MODIFIES SQL DATA
BEGIN
    UPDATE blocks
    SET parent_block_id = NULL
    WHERE block_id IN (SELECT id FROM JSON_TABLE(p_block_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 블록 삭제 (하위 블록·댓글·데이터베이스는 ON DELETE CASCADE)
--   p_block_id  블록 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_delete;

DELIMITER //

CREATE PROCEDURE sp_block_delete(IN p_block_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM blocks
    WHERE block_id = p_block_id;
END //

DELIMITER ;


-- 여러 블록 삭제
--   p_block_ids  블록 번호 목록 (JSON 배열)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_block_list_delete;

DELIMITER //

CREATE PROCEDURE sp_block_list_delete(IN p_block_ids JSON)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM blocks
    WHERE block_id IN (SELECT id FROM JSON_TABLE(p_block_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;
