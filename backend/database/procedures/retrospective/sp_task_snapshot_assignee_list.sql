-- 작업 스냅샷 담당자 (프로필 사진 주소 포함, 범위는 sp_task_snapshot_list와 동일)
--   p_workspace_id      워크스페이스 번호
--   p_retrospective_id  회고 번호 (NULL이면 워크스페이스 전체)
-- 결과: 담당자당 한 행, snapshot_assignee_id 순

DROP PROCEDURE IF EXISTS sp_task_snapshot_assignee_list;

DELIMITER //

CREATE PROCEDURE sp_task_snapshot_assignee_list(IN p_workspace_id BIGINT, IN p_retrospective_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT a.snapshot_id,
           a.original_user_id,
           a.nickname,
           a.profile_file_id,
           f.file_url    AS profile_image_url
      FROM task_snapshot_assignees a
      JOIN task_snapshots s  ON s.snapshot_id = a.snapshot_id
      JOIN retrospectives r  ON r.retrospective_id = s.retrospective_id
      JOIN sprints sp        ON sp.sprint_id = r.sprint_id
      LEFT JOIN files f      ON f.file_id = a.profile_file_id
     WHERE sp.workspace_id = p_workspace_id
       AND (p_retrospective_id IS NULL OR s.retrospective_id = p_retrospective_id)
     ORDER BY a.snapshot_assignee_id;
END //

DELIMITER ;
