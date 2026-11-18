package com.flowspace.user.dto.request;

import jakarta.validation.constraints.NotBlank;

// @formatter:off
public record EmailVerifyRequest(

    @NotBlank
    String token

) {}
// @formatter:on
