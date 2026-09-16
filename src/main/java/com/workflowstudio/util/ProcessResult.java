package com.workflowstudio.util;

public record ProcessResult(int exitCode, String stdout, String stderr) {
    public boolean success() {
        return exitCode == 0;
    }
}
