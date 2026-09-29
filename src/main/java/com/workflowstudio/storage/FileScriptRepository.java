package com.workflowstudio.storage;

import com.workflowstudio.util.AppPaths;
import com.workflowstudio.util.ScriptKeepAliveTransformer;
import com.workflowstudio.util.ScriptMaximizeTransformer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class FileScriptRepository implements ScriptRepository {
    private final AppPaths appPaths;

    public FileScriptRepository(AppPaths appPaths) {
        this.appPaths = Objects.requireNonNull(appPaths);
    }

    @Override
    public String loadScript(Path relativeScriptPath) {
        try {
            Path absolutePath = appPaths.root().resolve(relativeScriptPath);
            return Files.readString(absolutePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load script.", e);
        }
    }

    @Override
    public void saveScript(Path relativeScriptPath, String script) {
        Objects.requireNonNull(relativeScriptPath, "relativeScriptPath is required");
        Objects.requireNonNull(script, "script is required");
        try {
            Path absolutePath = appPaths.root().resolve(relativeScriptPath);
            Files.createDirectories(absolutePath.getParent());

            String finalScript = withMaximizedBrowser(withBrowserKeepAlive(script));

            Files.writeString(absolutePath, finalScript, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save script.", e);
        }
    }

    @Override
    public boolean deleteScript(Path relativeScriptPath) {
        try {
            Path absolutePath = appPaths.root().resolve(relativeScriptPath);
            return Files.deleteIfExists(absolutePath);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete script.", e);
        }
    }

    private String withBrowserKeepAlive(String script) {
        return ScriptKeepAliveTransformer.apply(script);
    }

    private String withMaximizedBrowser(String script) {
        return ScriptMaximizeTransformer.apply(script);
    }
}
