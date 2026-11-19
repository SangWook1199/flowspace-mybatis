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
