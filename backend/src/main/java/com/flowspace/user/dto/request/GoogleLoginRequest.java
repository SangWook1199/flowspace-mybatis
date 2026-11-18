// @formatter:off
package com.flowspace.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(

    @NotBlank
    String idToken

) {}
// @formatter:on