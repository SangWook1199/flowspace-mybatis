-- 본문 검색 후보 블록 (휴지통 페이지 제외, 블록 내용 JSON을 글자로 바꿔 LIKE)
--   p_workspace_id  워크스페이스 번호
--   p_like          LIKE 패턴 (소문자, '!'로 특수문자 이스케이프)
-- 결과: 최대 100행, block_id 역순 (id·page_id·body·페이지 title·icon)

DROP PROCEDURE IF EXISTS sp_search_block_list;

DELIMITER //

CREATE PROCEDURE sp_search_block_list(IN p_workspace_id BIGINT, IN p_like VARCHAR(255))
    READS SQL DATA
BEGIN
    SELECT b.block_id AS id,
           b.page_id,
           b.content  AS body,
           p.title,
           p.icon
      FROM blocks b
      JOIN pages p ON p.page_id = b.page_id
     WHERE p.workspace_id = p_workspace_id
       AND p.is_deleted = FALSE
       AND LOWER(CAST(b.content AS CHAR)) LIKE p_like ESCAPE '!'
     ORDER BY b.block_id DESC
     LIMIT 100;
END //

DELIMITER ;
