package com.flowspace.user.dto.request;

import jakarta.validation.constraints.NotBlank;

// Swagger OAuth2 로그인 요청 DTO
public record TokenRequest(

        @NotBlank String username,

        @NotBlank String password) {
}