package com.workflowstudio.view;

import com.workflowstudio.app.ApplicationContext;
import com.workflowstudio.controller.HomeController;
import com.workflowstudio.model.ApplicationSettings;
import com.workflowstudio.theme.Theme;
import com.workflowstudio.theme.ThemeManager;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class HomeView {
    private final ApplicationContext context;
    private final ThemeManager themeManager;
    private final ApplicationSettings settings;
    private final HomeController homeController;

    public HomeView(
            ApplicationContext context,
            ThemeManager themeManager,
            ApplicationSettings settings,
            Runnable openRecorderAction,
            Runnable openProjectsAction
    ) {
        this.context = Objects.requireNonNull(context);
        this.themeManager = Objects.requireNonNull(themeManager);
        this.settings = Objects.requireNonNull(settings);
        this.homeController = new HomeController(openRecorderAction, openProjectsAction);
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("home-root");
        root.setPadding(new Insets(20, 24, 8, 24));

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.getStyleClass().add("top-bar");
        Theme[] currentTheme = {settings.theme()};
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
        topBar.getChildren().add(themeBtn);
        root.setTop(topBar);

        VBox center = new VBox(14);
        center.getStyleClass().add("home-center");
        center.setAlignment(Pos.CENTER);

        Label appIcon = new Label("\u2699");
        appIcon.getStyleClass().add("hero-icon");
        Label title = new Label("Workflow Studio");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Record \u2022 Organize \u2022 Replay");
        subtitle.getStyleClass().add("app-subtitle");

        HBox actions = new HBox(28);
        actions.setAlignment(Pos.CENTER);
        Button webRecorder = createActionCard("\uD83C\uDFAC", "Web Recorder", "Record browser workflows");
        Button projects = createActionCard("\uD83D\uDCC1", "Projects", "Manage and replay workflows");
        actions.getChildren().addAll(webRecorder, projects);

        center.getChildren().addAll(appIcon, title, subtitle, actions);
        root.setCenter(center);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.getStyleClass().add("home-footer");
        Label version = new Label("v1.0 | Developed by Suresh B (DOMS)");
        version.getStyleClass().add("app-meta");
        footer.getChildren().add(version);
        root.setBottom(footer);

        webRecorder.setOnAction(e -> homeController.openRecorder());
        projects.setOnAction(e -> homeController.openProjects());

        return new Scene(root, 980, 680);
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

    private Button createActionCard(String icon, String title, String description) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("home-card-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("home-card-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("home-card-subtitle");

        VBox content = new VBox(10, iconLabel, titleLabel, descriptionLabel);
        content.setAlignment(Pos.CENTER);

        Button card = new Button();
        card.setGraphic(content);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.getStyleClass().add("home-action-card");
        card.setPrefSize(250, 180);
        card.setMinSize(250, 180);
        return card;
    }
}
