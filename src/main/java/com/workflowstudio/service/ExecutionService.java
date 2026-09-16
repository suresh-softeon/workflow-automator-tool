package com.workflowstudio.service;

import com.workflowstudio.playback.ExecutionSession;
import java.util.function.Consumer;

public interface ExecutionService {
    ExecutionSession executeWorkflow(String project, String submodule, String screenName, Consumer<String> logConsumer);
}
