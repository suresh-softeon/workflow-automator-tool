package com.workflowstudio.view;

import com.workflowstudio.app.ApplicationContext;
import com.workflowstudio.recorder.RecordingSession;
import com.workflowstudio.theme.Theme;
import com.workflowstudio.theme.ThemeManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public final class RecorderView {
    private final ApplicationContext context;
    private final ThemeManager themeManager;
    private final Theme initialTheme;
    private final Runnable backToHomeAction;
    private CodeArea generatedCodeArea;
    private TextArea recordingConsole;

    public RecorderView(
            ApplicationContext context,
            ThemeManager themeManager,
            Theme initialTheme,
            Runnable backToHomeAction
    ) {
        this.context = Objects.requireNonNull(context);
        this.themeManager = Objects.requireNonNull(themeManager);
        this.initialTheme = Objects.requireNonNull(initialTheme);
        this.backToHomeAction = Objects.requireNonNull(backToHomeAction);
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("recorder-root");
        root.setPadding(new Insets(18, 18, 14, 18));

        HBox topBar = buildTopBar(root);
        root.setTop(topBar);

        HBox content = new HBox(12);
        VBox rightColumn = new VBox(12, buildScriptPanel(), buildConsolePanel());
        VBox.setVgrow(rightColumn.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        content.getChildren().addAll(buildWorkflowPanel(), rightColumn);
        root.setCenter(content);

        return new Scene(root, 1100, 760);
    }

    private HBox buildTopBar(BorderPane root) {
        Button backButton = new Button("Home");
        backButton.getStyleClass().add("secondary-action");
        backButton.setOnAction(e -> backToHomeAction.run());

        Label title = new Label("Web Recorder");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Theme[] currentTheme = {initialTheme};
        Button themeBtn = new Button(themeIcon(currentTheme[0]));
        themeBtn.getStyleClass().add("theme-toggle-btn");
        themeBtn.setTooltip(new Tooltip(currentTheme[0].label()));
        themeBtn.setOnAction(e -> {
            currentTheme[0] = nextTheme(currentTheme[0]);
            themeBtn.setText(themeIcon(currentTheme[0]));
            themeBtn.getTooltip().setText(currentTheme[0].label());
            context.settingsService().saveTheme(currentTheme[0]);
            themeManager.applyTheme(root.getScene(), currentTheme[0]);
        });

        HBox topBar = new HBox(10, backButton, title, spacer, themeBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");
        return topBar;
    }

    private VBox buildWorkflowPanel() {
        Label projectLabel = new Label("Project");
        ComboBox<String> projectCombo = new ComboBox<>();
        projectCombo.setPromptText("Select Project");
        projectCombo.setMaxWidth(Double.MAX_VALUE);
        Button addProjectButton = new Button("+");
        Button deleteProjectButton = new Button("-");
        addProjectButton.getStyleClass().add("mini-icon-button");
        deleteProjectButton.getStyleClass().add("mini-icon-button");
        HBox projectRow = new HBox(8, projectCombo, addProjectButton, deleteProjectButton);
        projectRow.getStyleClass().add("combo-row");
        HBox.setHgrow(projectCombo, Priority.ALWAYS);

        Label submoduleLabel = new Label("Submodule");
        ComboBox<String> submoduleCombo = new ComboBox<>();
        submoduleCombo.setPromptText("Select Submodule");
        submoduleCombo.setMaxWidth(Double.MAX_VALUE);
        Button addSubmoduleButton = new Button("+");
        Button deleteSubmoduleButton = new Button("-");
        addSubmoduleButton.getStyleClass().add("mini-icon-button");
        deleteSubmoduleButton.getStyleClass().add("mini-icon-button");
        HBox submoduleRow = new HBox(8, submoduleCombo, addSubmoduleButton, deleteSubmoduleButton);
        submoduleRow.getStyleClass().add("combo-row");
        HBox.setHgrow(submoduleCombo, Priority.ALWAYS);

        Label screenLabel = new Label("Screen Name");
        TextField screenName = new TextField();
        screenName.setPromptText("Enter screen name");

        Button start = new Button("\u25b6 Start Recording");
        Button stop = new Button("\u25a0 Stop Recording");
        Button save = new Button("\ud83d\udcbe Save");
        start.getStyleClass().add("primary-action");
        stop.getStyleClass().add("secondary-action");
        save.getStyleClass().add("secondary-action");
        stop.setDisable(true);
        save.setDisable(true);
        start.setMaxWidth(Double.MAX_VALUE);
        stop.setMaxWidth(Double.MAX_VALUE);
        save.setMaxWidth(Double.MAX_VALUE);

        VBox panel = new VBox(
                10,
                projectLabel,
                projectRow,
                submoduleLabel,
                submoduleRow,
                screenLabel,
                screenName,
                new Separator(),
                start,
                stop,
                save
        );
        panel.getStyleClass().addAll("left-panel", "workflow-panel");
        panel.setPrefWidth(340);
        AtomicReference<RecordingSession> activeSession = new AtomicReference<>();
        AtomicReference<String> lastGeneratedScript = new AtomicReference<>("");

        refreshProjects(projectCombo);

        projectCombo.setOnAction(e -> {
            String selectedProject = projectCombo.getValue();
            if (selectedProject == null) {
                submoduleCombo.setItems(FXCollections.observableArrayList());
                return;
            }
            refreshSubmodules(projectCombo, submoduleCombo, selectedProject);
        });

        addProjectButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Project");
            dialog.setHeaderText("Create Project");
            dialog.setContentText("Project name:");
            Optional<String> input = dialog.showAndWait();
            input.ifPresent(name -> {
                try {
                    context.projectCatalogService().addProject(name);
                    refreshProjects(projectCombo);
                    projectCombo.setValue(name.trim());
                    refreshSubmodules(projectCombo, submoduleCombo, name.trim());
                } catch (IllegalArgumentException ex) {
                    showError(ex.getMessage());
                }
            });
        });

        deleteProjectButton.setOnAction(e -> {
            String selectedProject = projectCombo.getValue();
            if (selectedProject == null || selectedProject.isBlank()) {
                showError("Select a project to delete.");
                return;
            }
            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Delete project \"" + selectedProject + "\" and all its submodules?",
                    ButtonType.CANCEL,
                    ButtonType.OK
            );
            confirm.setHeaderText("Delete Project");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    context.projectCatalogService().deleteProject(selectedProject);
                    refreshProjects(projectCombo);
                    submoduleCombo.setItems(FXCollections.observableArrayList());
                    projectCombo.setValue(null);
                } catch (IllegalArgumentException ex) {
                    showError(ex.getMessage());
                }
            }
        });

        addSubmoduleButton.setOnAction(e -> {
            String selectedProject = projectCombo.getValue();
            if (selectedProject == null || selectedProject.isBlank()) {
                showError("Select a project before adding a submodule.");
                return;
            }
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Submodule");
            dialog.setHeaderText("Create Submodule in " + selectedProject);
            dialog.setContentText("Submodule name:");
            Optional<String> input = dialog.showAndWait();
            input.ifPresent(name -> {
                try {
                    context.projectCatalogService().addSubmodule(selectedProject, name);
                    refreshSubmodules(projectCombo, submoduleCombo, selectedProject);
                    submoduleCombo.setValue(name.trim());
                } catch (IllegalArgumentException ex) {
                    showError(ex.getMessage());
                }
            });
        });

        deleteSubmoduleButton.setOnAction(e -> {
            String selectedProject = projectCombo.getValue();
            String selectedSubmodule = submoduleCombo.getValue();
            if (selectedProject == null || selectedProject.isBlank()) {
                showError("Select a project first.");
                return;
            }
            if (selectedSubmodule == null || selectedSubmodule.isBlank()) {
                showError("Select a submodule to delete.");
                return;
            }
            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Delete submodule \"" + selectedSubmodule + "\" from project \"" + selectedProject + "\"?",
                    ButtonType.CANCEL,
                    ButtonType.OK
            );
            confirm.setHeaderText("Delete Submodule");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    context.projectCatalogService().deleteSubmodule(selectedProject, selectedSubmodule);
                    refreshSubmodules(projectCombo, submoduleCombo, selectedProject);
                    submoduleCombo.setValue(null);
                } catch (IllegalArgumentException ex) {
                    showError(ex.getMessage());
                }
            }
        });

        start.setOnAction(e -> {
            String project = safeTrim(projectCombo.getValue());
            String submodule = safeTrim(submoduleCombo.getValue());
            String screen = safeTrim(screenName.getText());

            if (project.isBlank() || submodule.isBlank() || screen.isBlank()) {
                showError("Project, Submodule, and Screen Name are required.");
                return;
            }
            if (context.workflowService().screenExists(project, submodule, screen)) {
                showError("Screen already exists. Choose a different Screen Name.");
                return;
            }
            if (activeSession.get() != null) {
                showError("A recording session is already running.");
                return;
            }

            start.setDisable(true);
            stop.setDisable(true);
            save.setDisable(true);

            CompletableFuture
                    .supplyAsync(
                            () -> context.recordingService().startRecording(project, submodule, screen),
                            context.executorService()
                    )
                    .thenAccept(session -> Platform.runLater(() -> {
                        activeSession.set(session);
                        lastGeneratedScript.set("");
                        stop.setDisable(false);
                        startConsoleStreaming(session);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            start.setDisable(false);
                            stop.setDisable(true);
                            showError("Unable to start recording.\n\nPlaywright Codegen could not be started.\n\nPlease verify that Node.js and Playwright are installed correctly.");
                        });
                        return null;
                    });
        });

        stop.setOnAction(e -> {
            RecordingSession session = activeSession.get();
            if (session == null) {
                showError("No active recording session found.");
                return;
            }

            stop.setDisable(true);
            CompletableFuture
                    .supplyAsync(() -> context.recordingService().stopRecording(session), context.executorService())
                    .thenAccept(result -> Platform.runLater(() -> {
                        activeSession.set(null);
                        start.setDisable(false);
                        if (result.success()) {
                            lastGeneratedScript.set(result.generatedScript());
                            if (generatedCodeArea != null) {
                                generatedCodeArea.replaceText(result.generatedScript());
                            }
                            save.setDisable(false);
                        } else {
                            showError(result.errorMessage());
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            activeSession.set(null);
                            start.setDisable(false);
                            showError("Unable to stop recording.");
                        });
                        return null;
                    });
        });

        save.setOnAction(e -> {
            String project = safeTrim(projectCombo.getValue());
            String submodule = safeTrim(submoduleCombo.getValue());
            String screen = safeTrim(screenName.getText());
            String script = lastGeneratedScript.get();

            if (project.isBlank() || submodule.isBlank() || screen.isBlank()) {
                showError("Project, Submodule, and Screen Name are required.");
                return;
            }
            if (script.isBlank()) {
                showError("No generated script is available to save.");
                return;
            }

            save.setDisable(true);
            CompletableFuture
                    .runAsync(() -> context.workflowService().saveGeneratedWorkflow(project, submodule, screen, script), context.executorService())
                    .thenRun(() -> Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Workflow saved successfully.", ButtonType.OK);
                        alert.setHeaderText("Saved");
                        alert.showAndWait();
                        save.setDisable(false);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            save.setDisable(false);
                            showError(extractMessage(ex));
                        });
                        return null;
                    });
        });

        return panel;
    }

    private VBox buildScriptPanel() {
        Label sectionTitle = new Label("Generated Script");
        sectionTitle.getStyleClass().add("section-title");

        generatedCodeArea = new CodeArea();
        generatedCodeArea.getStyleClass().add("code-area");
        generatedCodeArea.setEditable(false);
        generatedCodeArea.setParagraphGraphicFactory(LineNumberFactory.get(generatedCodeArea));
        generatedCodeArea.replaceText("// Generated Playwright JavaScript will appear here.");

        VBox panel = new VBox(10, sectionTitle, new VirtualizedScrollPane<>(generatedCodeArea));
        panel.getStyleClass().addAll("right-panel", "script-panel");
        VBox.setVgrow(panel.getChildren().get(1), Priority.ALWAYS);
        return panel;
    }

    private VBox buildConsolePanel() {
        Label sectionTitle = new Label("Recording Console");
        sectionTitle.getStyleClass().add("section-title");
        recordingConsole = new TextArea();
        recordingConsole.setEditable(false);
        recordingConsole.getStyleClass().add("recorder-console");
        recordingConsole.setPromptText("Recording console output...");
        recordingConsole.setPrefRowCount(8);

        VBox panel = new VBox(10, sectionTitle, recordingConsole);
        panel.getStyleClass().addAll("right-panel", "console-panel");
        return panel;
    }

    private void startConsoleStreaming(RecordingSession session) {
        recordingConsole.clear();
        recordingConsole.appendText("Recording started...\n");
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(session.process().getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    final String l = line;
                    Platform.runLater(() -> recordingConsole.appendText(l + "\n"));
                }
            } catch (IOException ignored) {
                // process ended
            }
            Platform.runLater(() -> recordingConsole.appendText("Recording process ended.\n"));
        });
        reader.setDaemon(true);
        reader.start();
    }

    private void refreshProjects(ComboBox<String> projectCombo) {
        projectCombo.setItems(FXCollections.observableArrayList(context.projectCatalogService().listProjects()));
    }

    private void refreshSubmodules(ComboBox<String> projectCombo, ComboBox<String> submoduleCombo, String project) {
        submoduleCombo.setItems(FXCollections.observableArrayList(context.projectCatalogService().listSubmodules(project)));
        if (projectCombo.getValue() != null && !projectCombo.getValue().equals(project)) {
            projectCombo.setValue(project);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Action failed");
        alert.showAndWait();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String extractMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "Operation failed." : current.getMessage();
    }

    private static String themeIcon(Theme theme) {
        return switch (theme) {
            case DARK -> "\uD83C\uDF19";   // 🌙
            case LIGHT -> "\u2600";         // ☀
            case SYSTEM -> "\uD83D\uDDA5"; // 🖥
        };
    }

    private static Theme nextTheme(Theme current) {
        Theme[] values = Theme.values();
        return values[(current.ordinal() + 1) % values.length];
    }
}
