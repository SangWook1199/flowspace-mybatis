-- 마감 임박 알림 대상 (해당 마감일의 미완료 작업과 담당자, 오늘 이미 보낸 대상 제외)
--   p_end_date  마감일 (오늘 또는 내일)
--   p_since     이 시각 이후 발송분은 발송 완료로 간주 (오늘 0시)
-- 결과: (담당자, 작업)당 한 행

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
