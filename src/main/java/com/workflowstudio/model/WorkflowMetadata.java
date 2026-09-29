package com.workflowstudio.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record WorkflowMetadata(
        String project,
        String submodule,
        String screenName,
        String scriptPath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public WorkflowMetadata {
        Objects.requireNonNull(project, "project is required");
        Objects.requireNonNull(submodule, "submodule is required");
        Objects.requireNonNull(screenName, "screenName is required");
        Objects.requireNonNull(scriptPath, "scriptPath is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
    }
}
