package com.workflowstudio.storage;

import java.nio.file.Path;

public interface ScriptRepository {
    String loadScript(Path relativeScriptPath);

    void saveScript(Path relativeScriptPath, String script);

    boolean deleteScript(Path relativeScriptPath);
}
