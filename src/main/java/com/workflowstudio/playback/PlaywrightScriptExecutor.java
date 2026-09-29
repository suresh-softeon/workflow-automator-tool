package com.workflowstudio.playback;

import com.workflowstudio.util.AppPaths;
import com.workflowstudio.util.CommandResolver;
import com.workflowstudio.util.ScriptKeepAliveTransformer;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlaywrightScriptExecutor implements ScriptExecutor {
    private static final Logger log = LoggerFactory.getLogger(PlaywrightScriptExecutor.class);

    private final AppPaths appPaths;

    public PlaywrightScriptExecutor(AppPaths appPaths) {
        this.appPaths = Objects.requireNonNull(appPaths);
    }

    @Override
    public Process start(Path absoluteScriptPath) {
        if (absoluteScriptPath == null || !Files.exists(absoluteScriptPath)) {
            throw new IllegalArgumentException("Script file does not exist.");
        }
        sanitizeLegacyKeepAlivePlacement(absoluteScriptPath);

        List<String> command = List.of("node", absoluteScriptPath.toAbsolutePath().toString());
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        applyNodePath(processBuilder.environment());
        processBuilder.directory(absoluteScriptPath.toAbsolutePath().getParent().toFile());
        try {
            Process process = processBuilder.start();
            log.info("Playback started for script {}. PID={}", absoluteScriptPath, process.pid());
            return process;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start playback process.", e);
        }
    }

    private void applyNodePath(Map<String, String> environment) {
        String npmGlobalRoot = resolveNpmGlobalRoot();
        String appNodeModules = appPaths.root().resolve("node_modules").toAbsolutePath().toString();
        String existingNodePath = environment.getOrDefault("NODE_PATH", "");

        List<String> paths = new ArrayList<>();
        paths.add(appNodeModules);
        if (!npmGlobalRoot.isBlank()) {
            paths.add(npmGlobalRoot);
        }
        if (!existingNodePath.isBlank()) {
            paths.add(existingNodePath);
        }

        environment.put("NODE_PATH", String.join(File.pathSeparator, paths));
    }

    private String resolveNpmGlobalRoot() {
        ProcessBuilder processBuilder = new ProcessBuilder(CommandResolver.resolve(List.of("npm", "root", "-g")));
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return "";
            }
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "";
        }
    }

    private void sanitizeLegacyKeepAlivePlacement(Path scriptPath) {
        try {
            String script = Files.readString(scriptPath, StandardCharsets.UTF_8);
            String normalized = ScriptKeepAliveTransformer.apply(script);
            if (!normalized.equals(script)) {
                Files.writeString(scriptPath, normalized, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to prepare script for playback.", e);
        }
    }
}
