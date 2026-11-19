-- 작업 관련 문서 프로시저 (TaskAttachment_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/task_attachment.sql"


-- 작업 관련 문서 목록 (페이지·파일 정보, 등록자 이름 포함, 휴지통 페이지 제외)
-- 페이지 연결(page_id)과 파일(file_id) 중 하나라서 둘 다 LEFT JOIN
--   p_task_id  작업 번호
-- 결과: 문서당 한 행, attachment_id 순

DROP PROCEDURE IF EXISTS sp_task_attachment_list;

DELIMITER //

CREATE PROCEDURE sp_task_attachment_list(IN p_task_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT a.attachment_id,
           a.task_id,
           a.page_id,
           a.file_id,
           a.created_by,
           a.created_at,
           u.nickname       AS created_by_name,
           p.title          AS page_title,
           p.icon           AS page_icon,
           p.is_deleted     AS page_deleted,
           f.original_name  AS file_name,
           f.file_url,
           f.size           AS file_size,
           f.mime_type
      FROM task_attachments a
      JOIN users u      ON u.user_id = a.created_by
      LEFT JOIN pages p ON p.page_id = a.page_id
      LEFT JOIN files f ON f.file_id = a.file_id
     WHERE a.task_id = p_task_id
       AND (a.page_id IS NULL OR p.is_deleted = FALSE)
     ORDER BY a.attachment_id;
END //

DELIMITER ;


-- 관련 문서 한 건 (컬럼은 sp_task_attachment_list와 동일)
--   p_attachment_id  관련 문서 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_task_attachment;

DELIMITER //

CREATE PROCEDURE sp_task_attachment(IN p_attachment_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT a.attachment_id,
           a.task_id,
           a.page_id,
           a.file_id,
           a.created_by,
           a.created_at,
           u.nickname       AS created_by_name,
           p.title          AS page_title,
           p.icon           AS page_icon,
           p.is_deleted     AS page_deleted,
           f.original_name  AS file_name,
           f.file_url,
           f.size           AS file_size,
           f.mime_type
      FROM task_attachments a
      JOIN users u      ON u.user_id = a.created_by
      LEFT JOIN pages p ON p.page_id = a.page_id
      LEFT JOIN files f ON f.file_id = a.file_id
     WHERE a.attachment_id = p_attachment_id;
END //

DELIMITER ;


-- 작업의 관련 문서 수
--   p_task_id  작업 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_task_attachment_count;

DELIMITER //

CREATE PROCEDURE sp_task_attachment_count(IN p_task_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM task_attachments
    WHERE task_id = p_task_id;
END //

DELIMITER ;


-- 페이지 연결 여부
--   p_task_id  작업 번호
--   p_page_id  페이지 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_task_attachment_page_count;

DELIMITER //

CREATE PROCEDURE sp_task_attachment_page_count(IN p_task_id BIGINT, IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM task_attachments
    WHERE task_id = p_task_id
      AND page_id = p_page_id;
END //

DELIMITER ;


-- 작업에 업로드한 파일 번호
--   p_task_id  작업 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_task_attachment_file_id_list;

DELIMITER //

CREATE PROCEDURE sp_task_attachment_file_id_list(IN p_task_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT file_id
    FROM task_attachments
    WHERE task_id = p_task_id
      AND file_id IS NOT NULL;
END //

DELIMITER ;


-- 연결할 페이지의 워크스페이스 (휴지통 페이지 제외)
--   p_page_id  페이지 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_active_page_workspace_id;

DELIMITER //

CREATE PROCEDURE sp_active_page_workspace_id(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT workspace_id
    FROM pages
    WHERE page_id = p_page_id
      AND is_deleted = FALSE;
END //

DELIMITER ;


-- 관련 문서 등록 (pageId, fileId 중 하나)
--   p_task_id        작업 번호
--   p_page_id        연결할 페이지 번호 (파일이면 NULL)
--   p_file_id        업로드한 파일 번호 (페이지면 NULL)
--   p_created_by     만든 사용자 번호
--   p_attachment_id  새 번호
-- 결과: 없음 (p_attachment_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_task_attachment_insert;

DELIMITER //

CREATE PROCEDURE sp_task_attachment_insert(IN p_task_id BIGINT, IN p_page_id BIGINT, IN p_file_id BIGINT,
                                           IN p_created_by BIGINT, OUT p_attachment_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO task_attachments (task_id, page_id, file_id, created_by)
    VALUES (p_task_id, p_page_id, p_file_id, p_created_by);

    SET p_attachment_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 관련 문서 삭제
--   p_attachment_id  관련 문서 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_task_attachment_delete;

DELIMITER //

CREATE PROCEDURE sp_task_attachment_delete(IN p_attachment_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM task_attachments
    WHERE attachment_id = p_attachment_id;
END //

DELIMITER ;
