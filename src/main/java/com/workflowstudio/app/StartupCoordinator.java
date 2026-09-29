package com.workflowstudio.app;

import com.workflowstudio.model.ApplicationSettings;
import com.workflowstudio.validation.ValidationResult;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StartupCoordinator {
    private static final Logger log = LoggerFactory.getLogger(StartupCoordinator.class);

    private final ApplicationContext context;

    public StartupCoordinator(ApplicationContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public void startupAsync(Consumer<StartupState> callback) {
        CompletableFuture
                .supplyAsync(this::performStartup, context.executorService())
                .thenAccept(startupState -> Platform.runLater(() -> callback.accept(startupState)));
    }

    private StartupState performStartup() {
        try {
            context.appPaths().ensureDirectories();
        } catch (IOException e) {
            return StartupState.failed(ValidationResult.startupFilesystemFailure(e));
        }

        ValidationResult validation = context.startupValidator().validate();
        if (!validation.success()) {
            return StartupState.failed(validation);
        }

        ApplicationSettings settings = context.settingsService().load();
        log.info("Startup completed successfully");
        return StartupState.success(settings);
    }
}
