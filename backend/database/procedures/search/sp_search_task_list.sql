-- 작업 검색 (제목·설명, 상태 이름 포함)
--   p_workspace_id  워크스페이스 번호
--   p_like          LIKE 패턴 (소문자, '!'로 특수문자 이스케이프)
--   p_limit         최대 행 수
-- 결과: task_id 역순 (id·title·body·meta(상태 이름)·sprint_id)

DROP PROCEDURE IF EXISTS sp_search_task_list;

DELIMITER //

CREATE PROCEDURE sp_search_task_list(IN p_workspace_id BIGINT, IN p_like VARCHAR(255), IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT t.task_id     AS id,
           t.title,
           t.description AS body,
           s.name        AS meta,
           t.sprint_id
      FROM tasks t
      JOIN task_statuses s ON s.status_id = t.status_id
     WHERE t.workspace_id = p_workspace_id
       AND (LOWER(t.title) LIKE p_like ESCAPE '!'
            OR LOWER(COALESCE(t.description, '')) LIKE p_like ESCAPE '!')
     ORDER BY t.task_id DESC
     LIMIT p_limit;
END //

DELIMITER ;
