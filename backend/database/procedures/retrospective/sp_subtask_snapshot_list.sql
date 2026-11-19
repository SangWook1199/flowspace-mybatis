-- 회고의 하위 작업 스냅샷 (담당자 사진 주소 포함)
--   p_retrospective_id  회고 번호
-- 결과: 하위 작업당 한 행, position 순

DROP PROCEDURE IF EXISTS sp_subtask_snapshot_list;

DELIMITER //

CREATE PROCEDURE sp_subtask_snapshot_list(IN p_retrospective_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT st.subtask_snapshot_id,
           st.snapshot_id,
           st.content,
           st.is_completed,
           st.assignee_name,
           st.assignee_profile_file_id,
           f.file_url    AS assignee_profile_image_url,
           st.position
      FROM subtask_snapshots st
      JOIN task_snapshots s  ON s.snapshot_id = st.snapshot_id
      LEFT JOIN files f      ON f.file_id = st.assignee_profile_file_id
     WHERE s.retrospective_id = p_retrospective_id
     ORDER BY st.position, st.subtask_snapshot_id;
END //

DELIMITER ;
