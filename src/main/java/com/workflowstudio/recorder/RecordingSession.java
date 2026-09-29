package com.workflowstudio.recorder;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RecordingSession {
    private final String sessionId;
    private final String project;
    private final String submodule;
    private final String screenName;
    private final Path temporaryScriptPath;
    private final Process process;
    private final Instant startTime;
    private volatile RecordingStatus status;

    public RecordingSession(
            String project,
            String submodule,
            String screenName,
            Path temporaryScriptPath,
            Process process
    ) {
        this.sessionId = UUID.randomUUID().toString();
        this.project = Objects.requireNonNull(project);
        this.submodule = Objects.requireNonNull(submodule);
        this.screenName = Objects.requireNonNull(screenName);
        this.temporaryScriptPath = Objects.requireNonNull(temporaryScriptPath);
        this.process = Objects.requireNonNull(process);
        this.startTime = Instant.now();
        this.status = RecordingStatus.STARTING;
    }

    public String sessionId() {
        return sessionId;
    }

    public String project() {
        return project;
    }

    public String submodule() {
        return submodule;
    }

    public String screenName() {
        return screenName;
    }

    public Path temporaryScriptPath() {
        return temporaryScriptPath;
    }

    public Process process() {
        return process;
    }

    public Instant startTime() {
        return startTime;
    }

    public RecordingStatus status() {
        return status;
    }

    public void updateStatus(RecordingStatus status) {
        this.status = Objects.requireNonNull(status);
    }
}
