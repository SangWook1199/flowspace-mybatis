-- 회고 작업 스냅샷 (상태 이름 포함)
--   p_workspace_id      워크스페이스 번호
--   p_retrospective_id  회고 번호 (NULL이면 워크스페이스 전체, 회고 목록 요약용)
-- 결과: 스냅샷당 한 행, 상태 순서 → position 순

DROP PROCEDURE IF EXISTS sp_task_snapshot_list;

DELIMITER //

CREATE PROCEDURE sp_task_snapshot_list(IN p_workspace_id BIGINT, IN p_retrospective_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT s.snapshot_id,
           s.retrospective_id,
           s.snapshot_status_id,
           ss.name       AS status_name,
           s.title,
           s.priority,
           s.position
      FROM task_snapshots s
      JOIN retrospective_status_snapshots ss ON ss.snapshot_status_id = s.snapshot_status_id
      JOIN retrospectives r                  ON r.retrospective_id = s.retrospective_id
      JOIN sprints sp                        ON sp.sprint_id = r.sprint_id
     WHERE sp.workspace_id = p_workspace_id
       AND (p_retrospective_id IS NULL OR s.retrospective_id = p_retrospective_id)
     ORDER BY s.retrospective_id, ss.position, s.position, s.snapshot_id;
END //

DELIMITER ;
