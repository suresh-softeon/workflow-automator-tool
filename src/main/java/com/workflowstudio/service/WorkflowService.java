package com.workflowstudio.service;

import com.workflowstudio.model.WorkflowMetadata;
import java.util.List;

public interface WorkflowService {
    List<WorkflowMetadata> listWorkflows();

    boolean screenExists(String project, String submodule, String screenName);

    String loadWorkflowScript(String project, String submodule, String screenName);

    void saveGeneratedWorkflow(String project, String submodule, String screenName, String generatedScript);

    boolean deleteWorkflow(String project, String submodule, String screenName);
}
