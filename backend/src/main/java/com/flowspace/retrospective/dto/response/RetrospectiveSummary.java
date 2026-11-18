package com.flowspace.retrospective.dto.response;

import java.util.List;

// @formatter:off

public record RetrospectiveSummary(

    int completionRate,
    int completedTask,
    int incompleteTask,
    int totalTask,
    List<ParticipantItem> participants

) { }

// @formatter:on