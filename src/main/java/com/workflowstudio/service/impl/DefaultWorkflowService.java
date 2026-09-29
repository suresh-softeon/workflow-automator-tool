package com.workflowstudio.service.impl;

import com.workflowstudio.model.WorkflowMetadata;
import com.workflowstudio.service.WorkflowService;
import com.workflowstudio.storage.ScriptRepository;
import com.workflowstudio.storage.WorkflowRepository;
import com.workflowstudio.validation.WorkflowValidator;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultWorkflowService implements WorkflowService {
    private static final Logger log = LoggerFactory.getLogger(DefaultWorkflowService.class);

    private final WorkflowRepository workflowRepository;
    private final ScriptRepository scriptRepository;
    private final WorkflowValidator workflowValidator;

    public DefaultWorkflowService(
            WorkflowRepository workflowRepository,
            ScriptRepository scriptRepository,
            WorkflowValidator workflowValidator
    ) {
        this.workflowRepository = Objects.requireNonNull(workflowRepository);
        this.scriptRepository = Objects.requireNonNull(scriptRepository);
        this.workflowValidator = Objects.requireNonNull(workflowValidator);
    }

    @Override
    public List<WorkflowMetadata> listWorkflows() {
        return workflowRepository.listAll();
    }

    @Override
    public boolean screenExists(String project, String submodule, String screenName) {
        return workflowRepository.find(project, submodule, screenName).isPresent();
    }

    @Override
    public String loadWorkflowScript(String project, String submodule, String screenName) {
        WorkflowMetadata metadata = workflowRepository.find(project, submodule, screenName)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found."));
        return scriptRepository.loadScript(Path.of(metadata.scriptPath()));
    }

    @Override
    public void saveGeneratedWorkflow(String project, String submodule, String screenName, String generatedScript) {
        workflowValidator.validateForCreate(project, submodule, screenName);
        workflowValidator.validateGeneratedScript(generatedScript);

        Path relativeScriptPath = Path.of("scripts", project, submodule, screenName + ".js");
        scriptRepository.saveScript(relativeScriptPath, generatedScript);
        WorkflowMetadata metadata = new WorkflowMetadata(
                project,
                submodule,
                screenName,
                relativeScriptPath.toString().replace('\\', '/'),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        workflowValidator.validateMetadata(metadata);
        workflowRepository.upsert(metadata);
        log.info("Workflow saved for {}/{}/{}", project, submodule, screenName);
    }

    @Override
    public boolean deleteWorkflow(String project, String submodule, String screenName) {
        return workflowRepository.find(project, submodule, screenName)
                .map(it -> {
                    scriptRepository.deleteScript(Path.of(it.scriptPath()));
                    boolean metadataDeleted = workflowRepository.delete(project, submodule, screenName);
                    log.info("Workflow deleted for {}/{}/{}", project, submodule, screenName);
                    return metadataDeleted;
                })
                .orElse(false);
    }
}
