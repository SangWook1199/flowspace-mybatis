-- 페이지 본인과 모든 하위 페이지 (휴지통 포함, WITH RECURSIVE)
--   p_page_id  시작 페이지 번호
-- 결과: 페이지당 한 행, depth(본인 0) → page_id 순

DROP PROCEDURE IF EXISTS sp_page_subtree;

DELIMITER //

CREATE PROCEDURE sp_page_subtree(IN p_page_id BIGINT)
    READS SQL DATA
BEGIN
    WITH RECURSIVE subtree AS (
        SELECT page_id, parent_page_id, is_deleted, deleted_at, 0 AS depth
          FROM pages
         WHERE page_id = p_page_id
        UNION ALL
        SELECT c.page_id, c.parent_page_id, c.is_deleted, c.deleted_at, s.depth + 1
          FROM pages c
          JOIN subtree s ON c.parent_page_id = s.page_id
    )
    SELECT page_id, parent_page_id, is_deleted, deleted_at, depth
      FROM subtree
     ORDER BY depth, page_id;
END //

DELIMITER ;
