package com.flowspace.file.service.impl;

import org.apache.ibatis.annotations.Mapper;

import com.flowspace.file.service.FileVO;

// 파일 DB 접근. 메서드 이름이 File_SQL.xml의 id와 같아야 해요.
@Mapper
public interface FileMapper {

    // 이메일로 사용자 번호 (올린 사람을 남길 때 써요, 없으면 null)
    Long selectUserIdByEmail(String email);

    // 파일 한 건 (없으면 null)
    FileVO selectFileById(Long fileId);

    // 파일 등록. 실행 후 DB가 만든 번호가 file의 fileId에 들어가요.
    int insertFile(FileVO file);

    // 파일 삭제. 이 파일을 쓰던 곳(프로필·커버·이미지 블록)은 DB가 NULL로 바꾸고, 관련 문서는 함께 지워요.
    int deleteFile(Long fileId);
}
