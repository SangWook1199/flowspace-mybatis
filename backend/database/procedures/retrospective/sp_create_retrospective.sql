-- 스프린트를 완료할 때 회고를 만들어요. 순서대로 하는 일:
--   1) 회고 페이지   2) 회고
--   3) 페이지 안의 Keep / Problem / Try 표 (블록 → 데이터베이스 → 열 3개 → 행 4개 → 빈 칸 12개)
--   4) 상태 스냅샷   5) 작업 스냅샷   6) 담당자 스냅샷   7) 하위 작업 스냅샷
-- 스냅샷은 완료하는 순간의 작업을 복사해 두는 거예요. 나중에 작업이 바뀌거나 지워져도 회고는 그대로 남아요.
-- 앞 단계에서 만든 번호를 다음 단계가 써야 해서(페이지 → 회고 → 표 …) 변수에 담아 이어 가요.
-- 한글·이모지(페이지 제목, 아이콘, 표 제목)는 문자 집합 문제를 피하려고 자바에서 인자로 넘겨요.
--   인자: p_sprint_id      완료하는 스프린트
--         p_workspace_id   그 스프린트의 워크스페이스
--         p_user_id        회고를 만든 사람 (스프린트를 완료한 사람)
--         p_page_title, p_page_icon, p_table_title
--   결과: p_page_id (OUT)  만든 회고 페이지 번호 (자바에서 활동 기록에 남겨요)

DROP PROCEDURE IF EXISTS sp_create_retrospective;

DELIMITER //

CREATE PROCEDURE sp_create_retrospective(IN p_sprint_id BIGINT, IN p_workspace_id BIGINT, IN p_user_id BIGINT,
                                         IN p_page_title VARCHAR(200), IN p_page_icon VARCHAR(20),
                                         IN p_table_title VARCHAR(100), OUT p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_retrospective_id BIGINT;
    DECLARE v_block_id BIGINT;
    DECLARE v_database_id BIGINT;

    -- 1) 회고 페이지 (최상위 페이지). LAST_INSERT_ID()는 방금 INSERT로 만든 번호예요.
    INSERT INTO pages (workspace_id, parent_page_id, title, icon, position, created_by, is_deleted, version)
    VALUES (p_workspace_id, NULL, p_page_title, p_page_icon, 0, p_user_id, FALSE, 0);
    SET p_page_id = LAST_INSERT_ID();

    -- 2) 회고
    INSERT INTO retrospectives (sprint_id, page_id)
    VALUES (p_sprint_id, p_page_id);
    SET v_retrospective_id = LAST_INSERT_ID();

    -- 3) Keep / Problem / Try 표
    INSERT INTO blocks (page_id, type, position, created_by)
    VALUES (p_page_id, 'DATABASE', 0, p_user_id);
    SET v_block_id = LAST_INSERT_ID();

    INSERT INTO block_databases (block_id, title, view_type)
    VALUES (v_block_id, p_table_title, 'TABLE');
    SET v_database_id = LAST_INSERT_ID();

    INSERT INTO block_database_columns (database_id, name, type, position, width)
    VALUES (v_database_id, 'Keep', 'TEXT', 0, 200),
           (v_database_id, 'Problem', 'TEXT', 1, 200),
           (v_database_id, 'Try', 'TEXT', 2, 200);

    INSERT INTO block_database_rows (database_id, position)
    VALUES (v_database_id, 0), (v_database_id, 1), (v_database_id, 2), (v_database_id, 3);

    -- 행 4개 × 열 3개 = 빈 칸 12개 (CROSS JOIN은 두 목록의 모든 짝을 만들어요)
    INSERT INTO block_database_cells (row_id, column_id, value)
    SELECT r.row_id, c.column_id, NULL
      FROM block_database_rows r
      CROSS JOIN block_database_columns c
     WHERE r.database_id = v_database_id
       AND c.database_id = v_database_id;

    -- 4) 상태 스냅샷: 워크스페이스의 작업 상태를 칸반 순서 그대로 복사해요
    INSERT INTO retrospective_status_snapshots (retrospective_id, original_status_id, name, color, position)
    SELECT v_retrospective_id, ts.status_id, ts.name, ts.color, wts.position
      FROM workspace_task_statuses wts
      JOIN task_statuses ts ON ts.status_id = wts.status_id
     WHERE wts.workspace_id = p_workspace_id
     ORDER BY wts.position;

    -- 5) 작업 스냅샷: 작업의 상태를 4)에서 만든 상태 스냅샷과 이어요
    INSERT INTO task_snapshots (retrospective_id, snapshot_status_id, original_task_id, title, priority, position)
    SELECT v_retrospective_id, ss.snapshot_status_id, t.task_id, t.title, t.priority, t.position
      FROM tasks t
      JOIN retrospective_status_snapshots ss
        ON ss.retrospective_id = v_retrospective_id
       AND ss.original_status_id = t.status_id
     WHERE t.sprint_id = p_sprint_id
     ORDER BY t.position, t.task_id;

    -- 6) 담당자 스냅샷: 그때의 닉네임과 프로필 사진을 남겨요
    INSERT INTO task_snapshot_assignees (snapshot_id, original_user_id, nickname, profile_file_id)
    SELECT s.snapshot_id, u.user_id, u.nickname, u.profile_file_id
      FROM task_snapshots s
      JOIN task_assignees ta ON ta.task_id = s.original_task_id
      JOIN users u           ON u.user_id = ta.user_id
     WHERE s.retrospective_id = v_retrospective_id
     ORDER BY ta.task_assignee_id;

    -- 7) 하위 작업 스냅샷 (담당자가 없을 수 있어서 LEFT JOIN)
    INSERT INTO subtask_snapshots (snapshot_id, original_subtask_id, content, is_completed, assignee_name,
                                   assignee_profile_file_id, position)
    SELECT s.snapshot_id, st.subtask_id, st.content, COALESCE(st.is_completed, FALSE), u.nickname,
           u.profile_file_id, COALESCE(st.position, 0)
      FROM task_snapshots s
      JOIN subtasks st    ON st.task_id = s.original_task_id
      LEFT JOIN users u   ON u.user_id = st.assignee_id
     WHERE s.retrospective_id = v_retrospective_id
     ORDER BY st.task_id, st.position;
END //

DELIMITER ;
