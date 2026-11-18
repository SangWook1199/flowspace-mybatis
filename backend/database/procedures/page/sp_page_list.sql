-- 워크스페이스의 페이지 목록(사이드바) 또는 휴지통 목록을 커버 주소·회고 여부와 함께 조회해요.
--   인자: p_workspace_id  워크스페이스 번호
--         p_deleted       FALSE면 사이드바 목록, TRUE면 휴지통 목록
--   결과: 페이지 한 개당 한 행.
--         사이드바는 순서(position) → 만든 순, 휴지통은 지운 시각이 늦은 순이에요.

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
           -- 회고 페이지인지 (회고 페이지는 화면이 따로 묶어 보여줘요)
           EXISTS (SELECT 1 FROM retrospectives r WHERE r.page_id = p.page_id) AS retrospective,
           -- 상위 페이지가 휴지통에 있는지 (복원할 때 써요, 상위가 없으면 NULL)
           parent.is_deleted AS parent_deleted
      FROM pages p
      LEFT JOIN files f      ON f.file_id = p.cover_file_id
      LEFT JOIN pages parent ON parent.page_id = p.parent_page_id
     WHERE p.workspace_id = p_workspace_id
       AND p.is_deleted = p_deleted
     -- 휴지통일 때만 첫 번째 정렬 값이 있어요 (사이드바일 때는 모두 NULL이라 다음 기준으로 정렬돼요)
     ORDER BY CASE WHEN p_deleted THEN p.deleted_at END DESC, p.position, p.created_at, p.page_id;
END //

DELIMITER ;
