package com.workflowstudio.util;

import java.time.Duration;
import java.util.List;

public interface ProcessExecutor extends AutoCloseable {
    ProcessResult execute(List<String> command, Duration timeout);

    @Override
    void close();
}
