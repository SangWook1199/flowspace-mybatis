package com.flowspace.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// @formatter:off
public record PasswordChangeRequest(

    @NotBlank
    String currentPassword,

    @NotBlank
    @Size(min = 8, max = 20)
    String newPassword

) {}
// @formatter:on
