package com.flowspace.file.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 파일 (files 테이블 한 행, 실제 파일은 file.upload-dir)
@Getter
@Setter
@NoArgsConstructor
public class FileVO {

    private Long fileId;

    private Long workspaceId;

    // 올린 사람
    private Long uploadedBy;

    // 업로드 시 원래 파일 이름 (화면 표시용)
    private String originalName;

    // 서버에 저장한 이름 (UUID + 확장자, 기본 커버는 커버 파일 이름)
    private String storedName;

    private String mimeType;

    private Long size;

    private Integer width;

    private Integer height;

    // /uploads/저장이름 (기본 커버는 /covers/파일이름)
    private String fileUrl;

    private LocalDateTime createdAt;
}
