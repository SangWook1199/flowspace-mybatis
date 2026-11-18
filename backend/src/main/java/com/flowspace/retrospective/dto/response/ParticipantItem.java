package com.flowspace.retrospective.dto.response;

// @formatter:off

public record ParticipantItem(

    Long userId,
    String name,
    Long profileFileId,
    String profileImageUrl

) { }

// @formatter:on