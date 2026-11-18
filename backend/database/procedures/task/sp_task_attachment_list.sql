-- 작업의 관련 문서 목록을 페이지·파일 정보, 올린 사람 이름과 함께 조회해요.
-- 관련 문서는 페이지 연결(page_id)과 파일(file_id) 중 하나라서 둘 다 LEFT JOIN 해요.
-- 휴지통에 있는 페이지는 빼요.
--   인자: p_task_id  작업 번호
--   결과: 문서 한 건당 한 행. 붙인 순서(attachment_id) 순.

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
           -- 페이지를 연결했을 때
           p.title          AS page_title,
           p.icon           AS page_icon,
           p.is_deleted     AS page_deleted,
           -- 파일을 올렸을 때
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
