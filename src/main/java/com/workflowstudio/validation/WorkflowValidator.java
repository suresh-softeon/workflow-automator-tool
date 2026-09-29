package com.workflowstudio.validation;

import com.workflowstudio.model.WorkflowMetadata;
import com.workflowstudio.storage.WorkflowRepository;
import java.util.Objects;

public final class WorkflowValidator {
    private final WorkflowRepository workflowRepository;

    public WorkflowValidator(WorkflowRepository workflowRepository) {
        this.workflowRepository = Objects.requireNonNull(workflowRepository);
    }

    public void validateForCreate(String project, String submodule, String screenName) {
        requireValue("project", project);
        requireValue("submodule", submodule);
        requireValue("screenName", screenName);

        boolean exists = workflowRepository.find(project, submodule, screenName).isPresent();
        if (exists) {
            throw new IllegalArgumentException("Screen already exists.");
        }
    }

    public void validateGeneratedScript(String script) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("Generated script is empty.");
        }
    }

    public void validateMetadata(WorkflowMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata is required");
        requireValue("project", metadata.project());
        requireValue("submodule", metadata.submodule());
        requireValue("screenName", metadata.screenName());
        requireValue("scriptPath", metadata.scriptPath());
    }

    private void requireValue(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
