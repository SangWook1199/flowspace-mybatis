-- 페이지 프로시저 (Page_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/page.sql"


-- 페이지 목록 또는 휴지통 목록 (커버 주소·회고 여부·상위 페이지 휴지통 여부 포함)
--   p_workspace_id  워크스페이스 번호
--   p_deleted       FALSE면 사이드바 목록, TRUE면 휴지통 목록
-- 결과: 페이지당 한 행 (사이드바: position → 생성 순, 휴지통: 삭제 시각 역순)

DROP PROCEDURE IF EXISTS sp_page_list;

DELIMITER //

CREATE PROCEDURE sp_page_list(IN p_workspace_id BIGINT, IN p_deleted BOOLEAN)
    READS SQL DATA
BEGIN
    SELECT p.page_id,
           p.workspace_id,
           p.parent_page_id,
           p.title,
           p.icon,
           p.cover_file_id,
           f.file_url      AS cover_url,
           p.position,
           p.created_by,
           p.is_deleted,
           p.deleted_at,
           p.version,
           p.created_at,
           p.updated_at,
           EXISTS (SELECT 1 FROM retrospectives r WHERE r.page_id = p.page_id) AS retrospective,
           parent.is_deleted AS parent_deleted
      FROM pages p
      LEFT JOIN files f      ON f.file_id = p.cover_file_id
      LEFT JOIN pages parent ON parent.page_id = p.parent_page_id
     WHERE p.workspace_id = p_workspace_id
       AND p.is_deleted = p_deleted
     ORDER BY CASE WHEN p_deleted THEN p.deleted_at END DESC, p.position, p.created_at, p.page_id;
END //

DELIMITER ;


-- 페이지 한 개 (휴지통 포함, 컬럼은 sp_page_list와 동일)
--   p_page_id  페이지 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_page;

DELIMITER //

CREATE PROCEDURE sp_page(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT p.page_id,
           p.workspace_id,
           p.parent_page_id,
           p.title,
           p.icon,
           p.cover_file_id,
           f.file_url      AS cover_url,
           p.position,
           p.created_by,
           p.is_deleted,
           p.deleted_at,
           p.version,
           p.created_at,
           p.updated_at,
           EXISTS (SELECT 1 FROM retrospectives r WHERE r.page_id = p.page_id) AS retrospective,
           parent.is_deleted AS parent_deleted
      FROM pages p
      LEFT JOIN files f      ON f.file_id = p.cover_file_id
      LEFT JOIN pages parent ON parent.page_id = p.parent_page_id
     WHERE p.page_id = p_page_id;
END //

DELIMITER ;


-- 페이지 본인과 모든 하위 페이지 (휴지통 포함, WITH RECURSIVE)
--   p_page_id  시작 페이지 번호
-- 결과: 페이지당 한 행, depth(본인 0) → page_id 순

DROP PROCEDURE IF EXISTS sp_page_subtree;

DELIMITER //

CREATE PROCEDURE sp_page_subtree(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    WITH RECURSIVE subtree AS (
        SELECT page_id, parent_page_id, title, icon, cover_file_id, position, is_deleted, deleted_at,
               0 AS depth
          FROM pages
         WHERE page_id = p_page_id
        UNION ALL
        SELECT c.page_id, c.parent_page_id, c.title, c.icon, c.cover_file_id, c.position, c.is_deleted,
               c.deleted_at, s.depth + 1
          FROM pages c
          JOIN subtree s ON c.parent_page_id = s.page_id
    )
    SELECT page_id, parent_page_id, title, icon, cover_file_id, position, is_deleted, deleted_at, depth
      FROM subtree
     ORDER BY depth, page_id;
END //

DELIMITER ;


-- 여러 페이지 (순서 바꾸기 확인용)
--   p_page_ids  페이지 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_page_list_by_ids;

DELIMITER //

CREATE PROCEDURE sp_page_list_by_ids(IN p_page_ids JSON)
    READS SQL DATA
BEGIN
    SELECT page_id,
           workspace_id,
           parent_page_id,
           is_deleted
    FROM pages
    WHERE page_id IN (SELECT id FROM JSON_TABLE(p_page_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 상위 페이지 번호
--   p_page_id  페이지 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_page_parent_id;

DELIMITER //

CREATE PROCEDURE sp_page_parent_id(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT parent_page_id
    FROM pages
    WHERE page_id = p_page_id;
END //

DELIMITER ;


-- 같은 상위 페이지 아래의 페이지 수 (<=>: NULL끼리 같음)
--   p_workspace_id    워크스페이스 번호
--   p_parent_page_id  상위 페이지 번호 (NULL이면 최상위)
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_page_sibling_count;

DELIMITER //

CREATE PROCEDURE sp_page_sibling_count(IN p_workspace_id BIGINT, IN p_parent_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM pages
    WHERE workspace_id = p_workspace_id
      AND parent_page_id <=> p_parent_page_id
      AND is_deleted = FALSE;
END //

DELIMITER ;


-- 페이지 등록 (휴지통 여부·버전·생성 시각은 DB 기본값)
--   p_workspace_id    워크스페이스 번호
--   p_parent_page_id  상위 페이지 번호 (NULL이면 최상위)
--   p_title           제목
--   p_icon            아이콘
--   p_cover_file_id   커버 파일 번호 (NULL이면 없음)
--   p_position        순서
--   p_created_by      만든 사용자 번호
--   p_page_id         새 번호
-- 결과: 없음 (p_page_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_page_insert;

DELIMITER //

CREATE PROCEDURE sp_page_insert(IN p_workspace_id BIGINT, IN p_parent_page_id BIGINT, IN p_title VARCHAR(200),
                                IN p_icon VARCHAR(20), IN p_cover_file_id BIGINT, IN p_position INT,
                                IN p_created_by BIGINT, OUT p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO pages (workspace_id, parent_page_id, title, icon, cover_file_id, position, created_by)
    VALUES (p_workspace_id, p_parent_page_id, p_title, p_icon, p_cover_file_id, p_position, p_created_by);

    SET p_page_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 제목·아이콘·상위 페이지 수정 (상위 페이지 null이면 최상위)
--   p_title           제목
--   p_icon            아이콘
--   p_parent_page_id  상위 페이지 번호 (NULL이면 최상위)
--   p_page_id         페이지 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_update;

DELIMITER //

CREATE PROCEDURE sp_page_update(IN p_title VARCHAR(200), IN p_icon VARCHAR(20), IN p_parent_page_id BIGINT,
                                IN p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE pages
    SET title          = p_title,
        icon           = p_icon,
        parent_page_id = p_parent_page_id
    WHERE page_id = p_page_id;
END //

DELIMITER ;


-- 순서 수정
--   p_position  순서
--   p_page_id   페이지 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_position_update;

DELIMITER //

CREATE PROCEDURE sp_page_position_update(IN p_position INT, IN p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE pages
    SET position = p_position
    WHERE page_id = p_page_id;
END //

DELIMITER ;


-- 상위 페이지와 순서 수정
--   p_parent_page_id  상위 페이지 번호 (NULL이면 최상위)
--   p_position        순서
--   p_page_id         페이지 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_parent_update;

DELIMITER //

CREATE PROCEDURE sp_page_parent_update(IN p_parent_page_id BIGINT, IN p_position INT, IN p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE pages
    SET parent_page_id = p_parent_page_id,
        position       = p_position
    WHERE page_id = p_page_id;
END //

DELIMITER ;


-- 커버 수정
--   p_file_id  커버 파일 번호 (NULL이면 없음)
--   p_page_id  페이지 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_cover_update;

DELIMITER //

CREATE PROCEDURE sp_page_cover_update(IN p_file_id BIGINT, IN p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE pages
    SET cover_file_id = p_file_id
    WHERE page_id = p_page_id;
END //

DELIMITER ;


-- 여러 페이지 휴지통 이동 (같은 삭제 시각 = 함께 복원되는 묶음)
--   p_deleted_at  휴지통 이동 시각
--   p_page_ids    페이지 번호 목록 (JSON 배열)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_trash_update;

DELIMITER //

CREATE PROCEDURE sp_page_trash_update(IN p_deleted_at DATETIME, IN p_page_ids JSON)
    MODIFIES SQL DATA
BEGIN
    UPDATE pages
    SET is_deleted = TRUE,
        deleted_at = p_deleted_at
    WHERE page_id IN (SELECT id FROM JSON_TABLE(p_page_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 여러 페이지 복원
--   p_page_ids  페이지 번호 목록 (JSON 배열)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_restore_update;

DELIMITER //

CREATE PROCEDURE sp_page_restore_update(IN p_page_ids JSON)
    MODIFIES SQL DATA
BEGIN
    UPDATE pages
    SET is_deleted = FALSE,
        deleted_at = NULL
    WHERE page_id IN (SELECT id FROM JSON_TABLE(p_page_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 휴지통으로 가는 페이지를 가리키는 데이터베이스 행 삭제 (함께 이동하는 페이지 안의 데이터베이스는 제외, 셀은 연쇄 삭제)
--   p_page_ids     페이지 번호 목록 (JSON 배열)
--   p_subtree_ids  함께 이동하는 하위 페이지 번호 목록 (JSON 배열)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_outside_database_row_delete;

DELIMITER //

CREATE PROCEDURE sp_page_outside_database_row_delete(IN p_page_ids JSON, IN p_subtree_ids JSON)
    MODIFIES SQL DATA
BEGIN
    DELETE r
      FROM block_database_rows r
      JOIN block_databases d ON d.database_id = r.database_id
      JOIN blocks b          ON b.block_id = d.block_id
     WHERE r.page_id IN (SELECT id FROM JSON_TABLE(p_page_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids)
       AND b.page_id NOT IN (SELECT id FROM JSON_TABLE(p_subtree_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 휴지통이 아닌 하위 페이지
--   p_page_id  페이지 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_page_child_list;

DELIMITER //

CREATE PROCEDURE sp_page_child_list(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT page_id,
           title,
           icon
    FROM pages
    WHERE parent_page_id = p_page_id
      AND is_deleted = FALSE
    ORDER BY page_id;
END //

DELIMITER ;


-- 보관 기간이 지난 휴지통 페이지 (모든 워크스페이스)
--   p_cutoff  이 시각 이전에 휴지통으로 간 페이지
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_page_expired_trash_list;

DELIMITER //

CREATE PROCEDURE sp_page_expired_trash_list(IN p_cutoff DATETIME)
    READS SQL DATA
BEGIN
    SELECT page_id,
           workspace_id,
           parent_page_id
    FROM pages
    WHERE is_deleted = TRUE
      AND deleted_at < p_cutoff;
END //

DELIMITER ;


-- 주어진 페이지 중 회고 페이지 번호
--   p_page_ids  페이지 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_page_retrospective_id_list;

DELIMITER //

CREATE PROCEDURE sp_page_retrospective_id_list(IN p_page_ids JSON)
    READS SQL DATA
BEGIN
    SELECT page_id
    FROM retrospectives
    WHERE page_id IN (SELECT id FROM JSON_TABLE(p_page_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 페이지 커버·블록 이미지 파일 번호 (UNION으로 중복 제외)
--   p_page_ids  페이지 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_page_file_id_list;

DELIMITER //

CREATE PROCEDURE sp_page_file_id_list(IN p_page_ids JSON)
    READS SQL DATA
BEGIN
    SELECT cover_file_id
    FROM pages
    WHERE cover_file_id IS NOT NULL
      AND page_id IN (SELECT id FROM JSON_TABLE(p_page_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids)
    UNION
    SELECT image_file_id
    FROM blocks
    WHERE image_file_id IS NOT NULL
      AND page_id IN (SELECT id FROM JSON_TABLE(p_page_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 페이지를 가리키는 데이터베이스 행 삭제
--   p_page_ids  페이지 번호 목록 (JSON 배열)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_database_row_delete;

DELIMITER //

CREATE PROCEDURE sp_page_database_row_delete(IN p_page_ids JSON)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM block_database_rows
    WHERE page_id IN (SELECT id FROM JSON_TABLE(p_page_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 페이지 삭제 (블록·데이터베이스는 CASCADE)
--   p_page_id  페이지 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_delete;

DELIMITER //

CREATE PROCEDURE sp_page_delete(IN p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM pages
    WHERE page_id = p_page_id;
END //

DELIMITER ;


-- ---------- 다른 Mapper 공용 ----------


-- 페이지 정보 + 트랜잭션 끝까지 행 잠금 (FOR UPDATE, 휴지통 제외)
--   p_page_id  페이지 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_page_lock;

DELIMITER //

CREATE PROCEDURE sp_page_lock(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT page_id,
           workspace_id,
           is_deleted,
           version
    FROM pages
    WHERE page_id = p_page_id
      AND is_deleted = FALSE
    FOR UPDATE;
END //

DELIMITER ;


-- 페이지 버전 +1
--   p_page_id  페이지 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_page_version_update;

DELIMITER //

CREATE PROCEDURE sp_page_version_update(IN p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE pages
    SET version = version + 1
    WHERE page_id = p_page_id;
END //

DELIMITER ;
