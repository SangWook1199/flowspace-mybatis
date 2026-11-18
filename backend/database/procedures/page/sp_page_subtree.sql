-- 페이지 본인과 모든 하위 페이지를 조회해요 (휴지통에 있는 하위 페이지도 포함).
-- WITH RECURSIVE: 본인에서 시작해서(첫 SELECT) "방금 찾은 페이지의 자식"을 더 찾을 게 없을 때까지 반복해요(둘째 SELECT).
--   인자: p_page_id  시작 페이지
--   결과: 페이지 한 개당 한 행. depth는 본인이 0, 자식이 1, 손자가 2 … (상위가 먼저 와요)

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
