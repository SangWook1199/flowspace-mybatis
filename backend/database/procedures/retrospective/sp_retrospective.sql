-- 회고 한 건을 스프린트 정보와 함께 조회해요. 컬럼은 sp_retrospective_list와 같아요(함께 고쳐요).
--   인자: p_retrospective_id  회고 번호
--   결과: 한 행 (없으면 0행)

DROP PROCEDURE IF EXISTS sp_retrospective;

DELIMITER //

CREATE PROCEDURE sp_retrospective(IN p_retrospective_id BIGINT)
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
      FROM retrospectives r
      JOIN sprints s ON s.sprint_id = r.sprint_id
     WHERE r.retrospective_id = p_retrospective_id;
END //

DELIMITER ;
