package com.workflowstudio.validation;

import com.workflowstudio.util.ProcessExecutor;
import com.workflowstudio.util.ProcessResult;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class NodeValidator {
    private final ProcessExecutor processExecutor;

    public NodeValidator(ProcessExecutor processExecutor) {
        this.processExecutor = Objects.requireNonNull(processExecutor);
    }

    public boolean isNodeAvailable() {
        ProcessResult result = processExecutor.execute(List.of("node", "-v"), Duration.ofSeconds(10));
        return result.success() && result.stdout().startsWith("v");
    }
}
