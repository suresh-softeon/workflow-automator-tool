package com.workflowstudio.view;

import com.workflowstudio.app.ApplicationContext;
import com.workflowstudio.model.WorkflowMetadata;
import com.workflowstudio.theme.Theme;
import com.workflowstudio.theme.ThemeManager;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public final class ProjectsView {
    private final ApplicationContext context;
    private final ThemeManager themeManager;
    private final Theme initialTheme;
    private final Runnable backToHomeAction;
    private final CodeArea previewCodeArea = buildPreviewCodeArea();
    private TreeView<TreeNodeValue> workflowTreeView;
    private Button playButton;
    private Button deleteButton;
    private HBox actionBar;
    private final TextArea executionConsole = new TextArea();

    public ProjectsView(
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
        root.getStyleClass().add("projects-root");
        root.setPadding(new Insets(18, 18, 14, 18));
        root.setTop(buildTopBar(root));

        HBox content = new HBox(12, buildTreePanel(), buildPreviewPanel());
        root.setCenter(content);
        HBox.setHgrow(content.getChildren().get(1), Priority.ALWAYS);

        return new Scene(root, 1100, 760);
    }

    private HBox buildTopBar(BorderPane root) {
        Button backButton = new Button("Home");
        backButton.getStyleClass().add("secondary-action");
        backButton.setOnAction(e -> backToHomeAction.run());

        Label title = new Label("Projects");
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

    private VBox buildTreePanel() {
        Label section = new Label("Workflow Tree");
        section.getStyleClass().add("section-title");

        TreeItem<TreeNodeValue> root = new TreeItem<>(TreeNodeValue.container("Projects"));
        root.setExpanded(true);
        workflowTreeView = new TreeView<>(root);
        workflowTreeView.setShowRoot(false);
        workflowTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(TreeNodeValue item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        workflowTreeView.setOnMouseClicked(event -> {
            TreeItem<TreeNodeValue> selected = workflowTreeView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue() == null) {
                return;
            }
            if (selected.getValue().optionalMetadata().isEmpty() && !selected.getChildren().isEmpty()) {
                selected.setExpanded(!selected.isExpanded());
            }
        });

        populateTree(root);
        workflowTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null || newItem.getValue() == null) {
                setActionButtonsEnabled(false);
                return;
            }
            Optional<WorkflowMetadata> metadata = newItem.getValue().optionalMetadata();
            if (metadata.isEmpty()) {
                setActionButtonsEnabled(false);
                previewCodeArea.replaceText("// Select a screen to preview saved script.");
                return;
            }
            setActionButtonsEnabled(true);
            try {
                WorkflowMetadata m = metadata.get();
                String script = context.workflowService().loadWorkflowScript(m.project(), m.submodule(), m.screenName());
                previewCodeArea.replaceText(script);
            } catch (IllegalStateException | IllegalArgumentException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
                alert.setHeaderText("Unable to load script");
                alert.showAndWait();
            }
        });

        VBox.setVgrow(workflowTreeView, Priority.ALWAYS);

        VBox panel = new VBox(10, section, workflowTreeView);
        panel.getStyleClass().add("left-panel");
        panel.setPrefWidth(320);
        return panel;
    }

    private VBox buildPreviewPanel() {
        Label section = new Label("Script Preview");
        section.getStyleClass().add("section-title");

        playButton = new Button("Play");
        deleteButton = new Button("Delete");
        playButton.getStyleClass().add("primary-action");
        deleteButton.getStyleClass().add("secondary-action");
        setActionButtonsEnabled(false);

        actionBar = new HBox(8, playButton, deleteButton);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setManaged(false);
        actionBar.setVisible(false);

        playButton.setOnAction(e -> playSelectedWorkflow());
        deleteButton.setOnAction(e -> deleteSelectedWorkflow());

        Label consoleLabel = new Label("Execution Console");
        consoleLabel.getStyleClass().add("section-title");
        executionConsole.setEditable(false);
        executionConsole.getStyleClass().add("recorder-console");
        executionConsole.setPrefRowCount(9);
        executionConsole.setPromptText("Playback logs will appear here...");

        VBox panel = new VBox(
                10,
                section,
                actionBar,
                new VirtualizedScrollPane<>(previewCodeArea),
                consoleLabel,
                executionConsole
        );
        panel.getStyleClass().add("right-panel");
        VBox.setVgrow(panel.getChildren().get(2), Priority.ALWAYS);
        return panel;
    }

    private CodeArea buildPreviewCodeArea() {
        CodeArea preview = new CodeArea();
        preview.setEditable(false);
        preview.setParagraphGraphicFactory(LineNumberFactory.get(preview));
        preview.replaceText("// Select a screen to preview saved script.");
        return preview;
    }

    private void populateTree(TreeItem<TreeNodeValue> root) {
        root.getChildren().clear();
        List<WorkflowMetadata> workflows = context.workflowService().listWorkflows();
        Map<String, Map<String, List<WorkflowMetadata>>> grouped = new LinkedHashMap<>();

        workflows.stream()
                .sorted(Comparator.comparing(WorkflowMetadata::project, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(WorkflowMetadata::submodule, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(WorkflowMetadata::screenName, String.CASE_INSENSITIVE_ORDER))
                .forEach(metadata -> grouped
                        .computeIfAbsent(metadata.project(), ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(metadata.submodule(), ignored -> new java.util.ArrayList<>())
                        .add(metadata));

        grouped.forEach((project, submodules) -> {
            TreeItem<TreeNodeValue> projectNode = new TreeItem<>(TreeNodeValue.container(project));
            projectNode.setExpanded(false);
            submodules.forEach((submodule, screens) -> {
                TreeItem<TreeNodeValue> submoduleNode = new TreeItem<>(TreeNodeValue.container(submodule));
                submoduleNode.setExpanded(false);
                screens.forEach(screen -> submoduleNode.getChildren().add(
                        new TreeItem<>(TreeNodeValue.workflow(screen.screenName(), screen))
                ));
                projectNode.getChildren().add(submoduleNode);
            });
            root.getChildren().add(projectNode);
        });

        if (root.getChildren().isEmpty()) {
            root.getChildren().add(new TreeItem<>(TreeNodeValue.container("(No saved workflows yet)")));
        }
    }

    private void setActionButtonsEnabled(boolean enabled) {
        if (playButton != null) {
            playButton.setDisable(!enabled);
        }
        if (deleteButton != null) {
            deleteButton.setDisable(!enabled);
        }
        if (actionBar != null) {
            actionBar.setManaged(enabled);
            actionBar.setVisible(enabled);
        }
    }

    private Optional<WorkflowMetadata> selectedWorkflow() {
        if (workflowTreeView == null) {
            return Optional.empty();
        }
        TreeItem<TreeNodeValue> selected = workflowTreeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) {
            return Optional.empty();
        }
        return selected.getValue().optionalMetadata();
    }

    private void playSelectedWorkflow() {
        Optional<WorkflowMetadata> selected = selectedWorkflow();
        if (selected.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Select a workflow screen first.", ButtonType.OK);
            alert.setHeaderText("Play");
            alert.showAndWait();
            return;
        }
        WorkflowMetadata metadata = selected.get();
        playButton.setDisable(true);
        executionConsole.clear();
        CompletableFuture
                .runAsync(
                        () -> context.executionService().executeWorkflow(
                                metadata.project(),
                                metadata.submodule(),
                                metadata.screenName(),
                                line -> Platform.runLater(() -> executionConsole.appendText(line + System.lineSeparator()))
                        ),
                        context.executorService()
                )
                .thenRun(() -> Platform.runLater(() -> playButton.setDisable(false)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        playButton.setDisable(false);
                        Alert alert = new Alert(Alert.AlertType.ERROR, extractMessage(ex), ButtonType.OK);
                        alert.setHeaderText("Unable to start playback");
                        alert.showAndWait();
                    });
                    return null;
                });
    }

    private void deleteSelectedWorkflow() {
        Optional<WorkflowMetadata> selected = selectedWorkflow();
        if (selected.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Select a workflow screen first.", ButtonType.OK);
            alert.setHeaderText("Delete");
            alert.showAndWait();
            return;
        }
        WorkflowMetadata metadata = selected.get();

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete \"" + metadata.screenName() + "\"?\n\nThis will permanently delete the workflow script and metadata.",
                ButtonType.CANCEL,
                new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE)
        );
        confirm.setHeaderText("Delete Workflow");
        Optional<ButtonType> decision = confirm.showAndWait();
        if (decision.isEmpty() || decision.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        boolean deleted = context.workflowService()
                .deleteWorkflow(metadata.project(), metadata.submodule(), metadata.screenName());
        if (!deleted) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Workflow not found.", ButtonType.OK);
            alert.setHeaderText("Delete failed");
            alert.showAndWait();
            return;
        }

        TreeItem<TreeNodeValue> root = workflowTreeView.getRoot();
        populateTree(root);
        previewCodeArea.replaceText("// Select a screen to preview saved script.");
        setActionButtonsEnabled(false);
    }

    private String extractMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "Operation failed." : current.getMessage();
    }

    private record TreeNodeValue(String label, WorkflowMetadata metadata) {
        static TreeNodeValue container(String label) {
            return new TreeNodeValue(label, null);
        }

        static TreeNodeValue workflow(String label, WorkflowMetadata metadata) {
            return new TreeNodeValue(label, metadata);
        }

        Optional<WorkflowMetadata> optionalMetadata() {
            return Optional.ofNullable(metadata);
        }
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
