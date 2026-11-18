package com.flowspace.file.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 파일 한 건이에요 (files 테이블 한 행). 실제 파일은 서버 폴더(file.upload-dir)에 있고, 여기에는 그 정보만 있어요.
@Getter
@Setter
@NoArgsConstructor
public class FileVO {

    private Long fileId;

    private Long workspaceId;

    // 올린 사람
    private Long uploadedBy;

    // 사용자가 올린 이름 (화면에 보여줘요)
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
