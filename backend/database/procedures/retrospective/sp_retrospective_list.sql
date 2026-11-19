-- 워크스페이스 스프린트 + 회고 목록 (회고가 없는 스프린트 포함)
--   p_workspace_id  워크스페이스 번호
-- 결과: 스프린트당 한 행 (회고가 없으면 retrospective_id, page_id는 NULL), 시작일 역순

DROP PROCEDURE IF EXISTS sp_retrospective_list;

DELIMITER //

CREATE PROCEDURE sp_retrospective_list(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT s.sprint_id,
           s.workspace_id,
           s.name        AS sprint_name,
           s.status      AS sprint_status,
           s.start_date,
           s.end_date,
           s.goal,
           r.retrospective_id,
           r.page_id,
           r.created_at
      FROM sprints s
      LEFT JOIN retrospectives r ON r.sprint_id = s.sprint_id
     WHERE s.workspace_id = p_workspace_id
     ORDER BY s.start_date DESC, s.sprint_id DESC;
END //

DELIMITER ;
