-- 그날이 마감인데 아직 끝나지 않은 작업과 그 담당자를 조회해요(마감 임박 알림을 보낼 대상).
-- 오늘 이미 같은 알림을 받은 (담당자, 작업)은 빼요(하루에 한 번만 보내게).
--   인자: p_end_date  마감일 (오늘 또는 내일)
--         p_since     이 시각 이후에 보낸 알림을 "이미 보냄"으로 봐요 (오늘 0시)
--   결과: (담당자, 작업) 한 쌍당 한 행

DROP PROCEDURE IF EXISTS sp_due_soon_target_list;

DELIMITER //

CREATE PROCEDURE sp_due_soon_target_list(IN p_end_date DATE, IN p_since DATETIME)
    READS SQL DATA
BEGIN
    SELECT ta.user_id,
           t.task_id,
           t.workspace_id,
           t.sprint_id,
           t.title
      FROM tasks t
      JOIN task_statuses ts  ON ts.status_id = t.status_id
      JOIN task_assignees ta ON ta.task_id = t.task_id
     WHERE t.end_date = p_end_date
       AND ts.category <> 'DONE'
       AND NOT EXISTS (SELECT 1
                         FROM notifications n
                        WHERE n.type = 'TASK_DUE_SOON'
                          AND n.user_id = ta.user_id
                          AND n.ref_id = t.task_id
                          AND n.created_at > p_since)
     ORDER BY t.task_id, ta.task_assignee_id;
END //

DELIMITER ;
