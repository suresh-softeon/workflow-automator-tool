package com.workflowstudio.recorder;

import com.workflowstudio.util.AppPaths;
import com.workflowstudio.util.CommandResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlaywrightCodegenRecorder implements CodegenRecorder {
    private static final Logger log = LoggerFactory.getLogger(PlaywrightCodegenRecorder.class);

    private final AppPaths appPaths;

    public PlaywrightCodegenRecorder(AppPaths appPaths) {
        this.appPaths = Objects.requireNonNull(appPaths);
    }

    @Override
    public RecordingSession startRecording(String project, String submodule, String screenName) {
        try {
            Path recordingDir = appPaths.dataDir().resolve("recordings");
            Files.createDirectories(recordingDir);
            Path tempScript = Files.createTempFile(recordingDir, "codegen-", ".js");

            List<String> command = List.of(
                    "npx",
                    "playwright",
                    "codegen",
                    "--target",
                    "javascript",
                    "--viewport-size",
                    "1920,1080",
                    "--output",
                    tempScript.toAbsolutePath().toString()
            );

            Process process = new ProcessBuilder(CommandResolver.resolve(command))
                    .redirectErrorStream(true)
                    .start();
            RecordingSession session = new RecordingSession(project, submodule, screenName, tempScript, process);
            session.updateStatus(RecordingStatus.RECORDING);
            log.info("Recording started: {} {} {} temp={}", project, submodule, screenName, tempScript);
            return session;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start recording. Playwright Codegen could not be started.", e);
        }
    }

    @Override
    public RecordingResult stopRecording(RecordingSession session) {
        session.updateStatus(RecordingStatus.STOPPING);
        Process process = session.process();
        try {
            if (process.isAlive()) {
                process.destroy();
                boolean exited = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
                if (!exited) {
                    process.destroyForcibly();
                    process.waitFor(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
                }
            }

            if (!Files.exists(session.temporaryScriptPath())) {
                session.updateStatus(RecordingStatus.FAILED);
                return RecordingResult.failure("Recording output file is missing.");
            }

            String script = Files.readString(session.temporaryScriptPath(), StandardCharsets.UTF_8);
            if (script.isBlank()) {
                session.updateStatus(RecordingStatus.FAILED);
                return RecordingResult.failure("Generated script is empty.");
            }

            session.updateStatus(RecordingStatus.COMPLETED);
            return RecordingResult.success(script);
        } catch (IOException e) {
            session.updateStatus(RecordingStatus.FAILED);
            return RecordingResult.failure("Unable to read generated script.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            session.updateStatus(RecordingStatus.FAILED);
            return RecordingResult.failure("Recording stop was interrupted.");
        }
    }
}
