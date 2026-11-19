package com.flowspace.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// @formatter:off
public record ProfileUpdateRequest(

    @NotBlank
    @Size(max = 30)
    String nickname,

    // 한 줄 소개 (비우면 삭제)
    @Size(max = 100)
    String bio

) {}