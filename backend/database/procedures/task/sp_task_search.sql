-- 제목으로 작업 검색 (대소문자 구분 없음, 상태 이름 포함)
--   p_workspace_id  워크스페이스 번호
--   p_keyword       검색어 (%, _는 Java에서 이스케이프, 빈 문자열이면 전체)
-- 결과: 작업당 한 행 (검색용 컬럼만)

DROP PROCEDURE IF EXISTS sp_task_search;

DELIMITER //

CREATE PROCEDURE sp_task_search(IN p_workspace_id BIGINT, IN p_keyword VARCHAR(200))
    READS SQL DATA
BEGIN
    SELECT t.task_id,
           t.title,
           t.status_id,
           ts.name AS status_name
      FROM tasks t
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE t.workspace_id = p_workspace_id
       AND LOWER(t.title) LIKE CONCAT('%', LOWER(p_keyword), '%')
     ORDER BY t.task_id;
END //

DELIMITER ;
