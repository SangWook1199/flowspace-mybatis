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
