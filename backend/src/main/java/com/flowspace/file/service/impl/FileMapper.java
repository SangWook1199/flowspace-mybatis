package com.flowspace.file.service.impl;

import org.apache.ibatis.annotations.Mapper;

import com.flowspace.file.service.FileVO;

// 파일 Mapper (File_SQL.xml)
@Mapper
public interface FileMapper {

    // 이메일로 사용자 번호 조회 (없으면 null)
    Long selectUserIdByEmail(String email);

    // 파일 한 건 (없으면 null)
    FileVO selectFileById(Long fileId);

    // 파일 등록 (생성된 번호는 fileId에 설정)
    int insertFile(FileVO file);

    // 파일 삭제 (프로필·커버·이미지 블록은 DB에서 NULL 처리, 관련 문서는 연쇄 삭제)
    int deleteFile(Long fileId);
}
