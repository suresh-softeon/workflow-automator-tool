package com.workflowstudio.playback;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class ExecutionSession {
    private final long processId;
    private final Instant startTime;
    private final AtomicReference<ExecutionStatus> status;
    private final StringBuilder stdoutBuffer;
    private final StringBuilder stderrBuffer;
    private volatile Instant endTime;
    private volatile Integer exitCode;

    public ExecutionSession(long processId, Instant startTime) {
        this.processId = processId;
        this.startTime = Objects.requireNonNull(startTime);
        this.status = new AtomicReference<>(ExecutionStatus.STARTING);
        this.stdoutBuffer = new StringBuilder();
        this.stderrBuffer = new StringBuilder();
    }

    public long processId() {
        return processId;
    }

    public Instant startTime() {
        return startTime;
    }

    public ExecutionStatus status() {
        return status.get();
    }

    public void updateStatus(ExecutionStatus status) {
        this.status.set(Objects.requireNonNull(status));
    }

    public synchronized void appendStdout(String line) {
        stdoutBuffer.append(line).append(System.lineSeparator());
    }

    public synchronized void appendStderr(String line) {
        stderrBuffer.append(line).append(System.lineSeparator());
    }

    public synchronized String stdout() {
        return stdoutBuffer.toString();
    }

    public synchronized String stderr() {
        return stderrBuffer.toString();
    }

    public Optional<Instant> endTime() {
        return Optional.ofNullable(endTime);
    }

    public void markFinished(int exitCode) {
        this.exitCode = exitCode;
        this.endTime = Instant.now();
        updateStatus(exitCode == 0 ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED);
    }

    public Optional<Integer> exitCode() {
        return Optional.ofNullable(exitCode);
    }
}
