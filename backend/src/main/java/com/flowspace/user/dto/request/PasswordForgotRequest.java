package com.flowspace.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// @formatter:off
public record PasswordForgotRequest(

    @NotBlank @Email @Size(max = 100)
    String email

) {}
// @formatter:on
