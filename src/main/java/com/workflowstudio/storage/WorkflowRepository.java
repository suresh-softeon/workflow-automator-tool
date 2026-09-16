package com.workflowstudio.storage;

import com.workflowstudio.model.WorkflowMetadata;
import java.util.List;
import java.util.Optional;

public interface WorkflowRepository {
    List<WorkflowMetadata> listAll();

    Optional<WorkflowMetadata> find(String project, String submodule, String screenName);

    void upsert(WorkflowMetadata metadata);

    boolean delete(String project, String submodule, String screenName);
}
