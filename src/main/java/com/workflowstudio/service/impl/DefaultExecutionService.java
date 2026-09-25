package com.workflowstudio.service.impl;

import com.workflowstudio.playback.ExecutionSession;
import com.workflowstudio.playback.ExecutionStatus;
import com.workflowstudio.playback.ScriptExecutor;
import com.workflowstudio.service.ExecutionService;
import com.workflowstudio.storage.WorkflowRepository;
import com.workflowstudio.util.AppPaths;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class DefaultExecutionService implements ExecutionService {
    private final WorkflowRepository workflowRepository;
    private final ScriptExecutor scriptExecutor;
    private final AppPaths appPaths;
    private final ExecutorService executorService;
    private final AtomicReference<Process> activeProcess = new AtomicReference<>();

    public DefaultExecutionService(
            WorkflowRepository workflowRepository,
            ScriptExecutor scriptExecutor,
            AppPaths appPaths,
            ExecutorService executorService
    ) {
        this.workflowRepository = Objects.requireNonNull(workflowRepository);
        this.scriptExecutor = Objects.requireNonNull(scriptExecutor);
        this.appPaths = Objects.requireNonNull(appPaths);
        this.executorService = Objects.requireNonNull(executorService);
    }

    @Override
    public ExecutionSession executeWorkflow(
            String project,
            String submodule,
            String screenName,
            Consumer<String> logConsumer
    ) {
        Path scriptPath = workflowRepository.find(project, submodule, screenName)
                .map(metadata -> appPaths.root().resolve(metadata.scriptPath()))
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found."));

        Process process;
        synchronized (this) {
            terminateActivePlayback(logConsumer);
            process = scriptExecutor.start(scriptPath);
            activeProcess.set(process);
        }
        ExecutionSession session = new ExecutionSession(process.pid(), Instant.now());
        session.updateStatus(ExecutionStatus.RUNNING);
        logConsumer.accept("[INFO] Playback started. PID=" + process.pid() + " at " + session.startTime());

        executorService.submit(() -> streamLines(process.getInputStream(), line -> {
            session.appendStdout(line);
            logConsumer.accept("[OUT] " + line);
        }));
        executorService.submit(() -> streamLines(process.getErrorStream(), line -> {
            session.appendStderr(line);
            logConsumer.accept("[ERR] " + line);
        }));
        executorService.submit(() -> {
            try {
                int exitCode = process.waitFor();
                session.markFinished(exitCode);
                logConsumer.accept("[INFO] Playback ended. exitCode=" + exitCode + ", endTime=" + session.endTime().orElse(null));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                session.updateStatus(ExecutionStatus.FAILED);
                process.destroyForcibly();
                logConsumer.accept("[ERR] Playback wait interrupted.");
            } finally {
                activeProcess.compareAndSet(process, null);
            }
        });

        return session;
    }

    private void terminateActivePlayback(Consumer<String> logConsumer) {
        Process existing = activeProcess.get();
        if (existing == null || !existing.isAlive()) {
            return;
        }

        logConsumer.accept("[INFO] Previous playback still running (PID=" + existing.pid() + "). Stopping it before new run.");
        ProcessHandle handle = existing.toHandle();
        List<ProcessHandle> descendants = new ArrayList<>(handle.descendants().collect(Collectors.toList()));
        for (ProcessHandle descendant : descendants) {
            descendant.destroy();
        }
        existing.destroy();

        try {
            if (!existing.waitFor(5, TimeUnit.SECONDS)) {
                for (ProcessHandle descendant : descendants) {
                    if (descendant.isAlive()) {
                        descendant.destroyForcibly();
                    }
                }
                existing.destroyForcibly();
                existing.waitFor(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            existing.destroyForcibly();
        }
    }

    private void streamLines(InputStream stream, Consumer<String> onLine) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                onLine.accept(line);
            }
        } catch (IOException e) {
            onLine.accept("Unable to read process output: " + e.getMessage());
        }
    }
}
