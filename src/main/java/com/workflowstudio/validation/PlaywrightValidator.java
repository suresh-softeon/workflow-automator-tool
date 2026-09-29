package com.workflowstudio.validation;

import com.workflowstudio.util.ProcessExecutor;
import com.workflowstudio.util.ProcessResult;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class PlaywrightValidator {
    private final ProcessExecutor processExecutor;

    public PlaywrightValidator(ProcessExecutor processExecutor) {
        this.processExecutor = Objects.requireNonNull(processExecutor);
    }

    public boolean isPlaywrightAvailable() {
        ProcessResult result = processExecutor.execute(List.of("npx", "playwright", "--version"), Duration.ofSeconds(20));
        return result.success() && !result.stdout().isBlank();
    }
}
