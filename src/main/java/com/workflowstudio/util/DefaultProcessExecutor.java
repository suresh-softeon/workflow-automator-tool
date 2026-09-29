package com.workflowstudio.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultProcessExecutor implements ProcessExecutor {
    private static final Logger log = LoggerFactory.getLogger(DefaultProcessExecutor.class);

    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();

    @Override
    public ProcessResult execute(List<String> command, Duration timeout) {
        List<String> resolvedCommand = CommandResolver.resolve(command);
        ProcessBuilder processBuilder = new ProcessBuilder(resolvedCommand);
        try {
            Process process = processBuilder.start();
            Future<String> stdoutFuture = ioExecutor.submit(() -> readAll(process.getInputStream()));
            Future<String> stderrFuture = ioExecutor.submit(() -> readAll(process.getErrorStream()));

            boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                return new ProcessResult(1, "", "Timed out");
            }

            String stdout = stdoutFuture.get();
            String stderr = stderrFuture.get();
            return new ProcessResult(process.exitValue(), stdout, stderr);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Command execution interrupted: {}", resolvedCommand, e);
            return new ProcessResult(1, "", "Execution interrupted");
        } catch (ExecutionException | IOException e) {
            log.error("Command execution failed: {}", resolvedCommand, e);
            return new ProcessResult(1, "", e.getMessage() == null ? "Execution failed" : e.getMessage());
        }
    }

    private String readAll(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.lineSeparator());
            }
        }
        return result.toString().trim();
    }

    @Override
    public void close() {
        ioExecutor.shutdownNow();
    }
}
