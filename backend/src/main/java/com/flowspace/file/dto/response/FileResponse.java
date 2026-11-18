package com.flowspace.file.dto.response;

import com.flowspace.file.service.FileVO;

// @formatter:off

// 파일 응답 DTO
public record FileResponse(

    Long fileId,
    String originalName,
    String fileUrl,
    String mimeType,
    Long size,
    Integer width,
    Integer height

) {

    public static FileResponse from(FileVO file) {
        return new FileResponse(
            file.getFileId(),
            file.getOriginalName(),
            file.getFileUrl(),
            file.getMimeType(),
            file.getSize(),
            file.getWidth(),
            file.getHeight()
        );
    }

}

// @formatter:on
