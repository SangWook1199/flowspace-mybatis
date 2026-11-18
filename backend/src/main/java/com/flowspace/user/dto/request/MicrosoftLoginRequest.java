// @formatter:off
package com.flowspace.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MicrosoftLoginRequest(

    @NotBlank
    String idToken,

    @NotBlank
    String accessToken

) {}
// @formatter:on