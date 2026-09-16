package com.workflowstudio.app;

import com.workflowstudio.model.ApplicationSettings;
import com.workflowstudio.theme.ThemeManager;
import com.workflowstudio.validation.ValidationResult;
import com.workflowstudio.view.HomeView;
import com.workflowstudio.view.ProjectsView;
import com.workflowstudio.view.RecorderView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApplicationLauncher extends Application {
    private static final Logger log = LoggerFactory.getLogger(ApplicationLauncher.class);

    private ApplicationContext applicationContext;
    private Stage primaryStage;
    private ApplicationSettings currentSettings;
    private ThemeManager themeManager;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        applicationContext = ApplicationContext.create();
        themeManager = applicationContext.themeManager();
        primaryStage.setTitle("Workflow Studio");

        Scene loadingScene = new Scene(new VBox(12, new Label("Workflow Studio"), new Label("Starting up...")), 980, 680);
        primaryStage.setScene(loadingScene);
        primaryStage.show();

        StartupCoordinator startupCoordinator = new StartupCoordinator(applicationContext);
        startupCoordinator.startupAsync(startupState -> {
            if (!startupState.validation().success()) {
                showStartupError(primaryStage, startupState.validation());
                return;
            }

            currentSettings = startupState.settings();
            showHome();
        });
    }

    private void showHome() {
        currentSettings = applicationContext.settingsService().load();
        HomeView homeView = new HomeView(
                applicationContext,
                themeManager,
                currentSettings,
                this::showRecorder,
                this::showProjects
        );
        Scene scene = homeView.createScene();
        themeManager.applyTheme(scene, currentSettings.theme());
        primaryStage.setScene(scene);
    }

    private void showRecorder() {
        currentSettings = applicationContext.settingsService().load();
        RecorderView recorderView = new RecorderView(
                applicationContext,
                themeManager,
                currentSettings.theme(),
                this::refreshAndShowHome
        );
        Scene scene = recorderView.createScene();
        themeManager.applyTheme(scene, currentSettings.theme());
        primaryStage.setScene(scene);
    }

    private void showProjects() {
        currentSettings = applicationContext.settingsService().load();
        ProjectsView projectsView = new ProjectsView(
                applicationContext,
                themeManager,
                currentSettings.theme(),
                this::refreshAndShowHome
        );
        Scene scene = projectsView.createScene();
        themeManager.applyTheme(scene, currentSettings.theme());
        primaryStage.setScene(scene);
    }

    private void refreshAndShowHome() {
        showHome();
    }

    private void showStartupError(Stage stage, ValidationResult result) {
        log.warn("Startup validation failed: {}", result.message());
        Label title = new Label(result.title());
        Label message = new Label(result.message());
        Button action = new Button(result.actionLabel());
        action.setOnAction(e -> applicationContext.platformUtils().openUrl(result.actionUrl()));
        Scene errorScene = new Scene(new VBox(12, title, message, action), 980, 680);
        stage.setScene(errorScene);
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.shutdown();
        }
    }
}
