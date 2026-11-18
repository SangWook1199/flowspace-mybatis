-- 페이지 한 개를 조회해요(휴지통에 있어도 나와요). 컬럼은 sp_page_list와 같아요(함께 고쳐요).
--   인자: p_page_id  페이지 번호
--   결과: 한 행 (없으면 0행)

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
           -- 회고 페이지인지 (회고 페이지는 화면이 따로 묶어 보여줘요)
           EXISTS (SELECT 1 FROM retrospectives r WHERE r.page_id = p.page_id) AS retrospective,
           -- 상위 페이지가 휴지통에 있는지 (복원할 때 써요, 상위가 없으면 NULL)
           parent.is_deleted AS parent_deleted
      FROM pages p
      LEFT JOIN files f      ON f.file_id = p.cover_file_id
      LEFT JOIN pages parent ON parent.page_id = p.parent_page_id
     WHERE p.page_id = p_page_id;
END //

DELIMITER ;
