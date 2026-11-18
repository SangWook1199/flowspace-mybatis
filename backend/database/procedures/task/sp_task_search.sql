-- 워크스페이스에서 제목에 검색어가 들어간 작업을 상태 이름과 함께 찾아요(대소문자 구분 없이).
--   인자: p_workspace_id  워크스페이스 번호
--         p_keyword       검색어. %와 _는 자바에서 앞에 \를 붙여서 넘겨요(글자 그대로 찾게).
--                         빈 문자열이면 모든 작업이 나와요.
--   결과: 작업 한 건당 한 행 (검색 결과에 필요한 컬럼만)

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
