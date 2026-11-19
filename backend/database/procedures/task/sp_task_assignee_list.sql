-- 칸반 작업 담당자 (이름·프로필 이미지 포함, 작업 조건은 sp_task_list와 동일)
--   p_workspace_id  워크스페이스 번호
--   p_sprint_id     스프린트 번호 (NULL이면 백로그)
-- 결과: 담당자당 한 행, 지정 순서

DROP PROCEDURE IF EXISTS sp_task_assignee_list;

DELIMITER //

CREATE PROCEDURE sp_task_assignee_list(IN p_workspace_id BIGINT, IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT ta.task_id,
           u.user_id,
           u.nickname,
           u.profile_file_id,
           f.file_url AS profile_image_url
      FROM task_assignees ta
      JOIN tasks t ON t.task_id = ta.task_id
      JOIN users u ON u.user_id = ta.user_id
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE t.workspace_id = p_workspace_id
       AND t.sprint_id <=> p_sprint_id
     ORDER BY ta.task_assignee_id;
END //

DELIMITER ;
