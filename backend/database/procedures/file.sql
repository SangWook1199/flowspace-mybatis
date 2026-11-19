-- 파일 프로시저 (File_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/file.sql"


-- 파일 한 건
--   p_file_id  파일 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_file;

DELIMITER //

CREATE PROCEDURE sp_file(IN p_file_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT file_id,
           workspace_id,
           uploaded_by,
           original_name,
           stored_name,
           mime_type,
           size,
           width,
           height,
           file_url,
           created_at
      FROM files
     WHERE file_id = p_file_id;
END //

DELIMITER ;


-- 파일 등록 (생성 시각은 DB 기본값)
--   p_workspace_id   워크스페이스 번호
--   p_uploaded_by    올린 사용자 번호
--   p_original_name  원본 파일 이름
--   p_stored_name    저장 파일 이름
--   p_mime_type      MIME 종류
--   p_size           크기 (byte)
--   p_width          이미지 너비 (이미지가 아니면 NULL)
--   p_height         이미지 높이 (이미지가 아니면 NULL)
--   p_file_url       파일 주소
--   p_file_id        새 파일 번호
-- 결과: 없음 (p_file_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_file_insert;

DELIMITER //

CREATE PROCEDURE sp_file_insert(IN p_workspace_id BIGINT, IN p_uploaded_by BIGINT,
                                IN p_original_name VARCHAR(255), IN p_stored_name VARCHAR(255),
                                IN p_mime_type VARCHAR(100), IN p_size BIGINT, IN p_width INT, IN p_height INT,
                                IN p_file_url VARCHAR(500), OUT p_file_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO files (workspace_id, uploaded_by, original_name, stored_name, mime_type, size, width, height,
                       file_url)
    VALUES (p_workspace_id, p_uploaded_by, p_original_name, p_stored_name, p_mime_type, p_size, p_width, p_height,
            p_file_url);

    SET p_file_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 파일 삭제 (참조하는 곳은 ON DELETE SET NULL / CASCADE)
--   p_file_id  파일 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_file_delete;

DELIMITER //

CREATE PROCEDURE sp_file_delete(IN p_file_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM files
     WHERE file_id = p_file_id;
END //

DELIMITER ;
