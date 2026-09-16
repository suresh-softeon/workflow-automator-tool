package com.workflowstudio.playback;

import java.nio.file.Path;

public interface ScriptExecutor {
    Process start(Path absoluteScriptPath);
}
