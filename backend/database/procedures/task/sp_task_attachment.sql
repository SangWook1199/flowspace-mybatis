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
