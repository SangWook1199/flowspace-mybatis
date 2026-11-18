-- 회고의 작업 스냅샷을 상태 이름과 함께 조회해요.
-- 회고 목록 화면은 워크스페이스의 모든 회고 요약이 필요해서, 회고 번호를 비우면 워크스페이스 전체를 읽어요.
--   인자: p_workspace_id      워크스페이스 번호
--         p_retrospective_id  회고 번호 (NULL이면 워크스페이스의 모든 회고)
--   결과: 스냅샷 한 건당 한 행. 상태 순서 → 칸반 순서(position) 순.

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
