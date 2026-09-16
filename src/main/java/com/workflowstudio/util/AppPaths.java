package com.workflowstudio.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class AppPaths {
    private static final String APP_NAME = "Workflow Studio";

    private final Path root;

    private AppPaths(Path root) {
        this.root = Objects.requireNonNull(root);
    }

    public static AppPaths resolve() {
        String overriddenRoot = System.getenv("WORKFLOW_STUDIO_HOME");
        if (overriddenRoot != null && !overriddenRoot.isBlank()) {
            Path path = Path.of(overriddenRoot);
            System.setProperty("workflow.studio.home", path.toString());
            return new AppPaths(path);
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String userProfile = System.getenv("USERPROFILE");
            if (userProfile == null || userProfile.isBlank()) {
                userProfile = System.getProperty("user.home");
            }
            Path path = Path.of(userProfile, APP_NAME);
            System.setProperty("workflow.studio.home", path.toString());
            return new AppPaths(path);
        }

        Path path = Path.of(System.getProperty("user.home"), APP_NAME);
        System.setProperty("workflow.studio.home", path.toString());
        return new AppPaths(path);
    }

    public void ensureDirectories() throws IOException {
        Files.createDirectories(root());
        Files.createDirectories(dataDir());
        Files.createDirectories(scriptsDir());
        Files.createDirectories(settingsDir());
        Files.createDirectories(logsDir());
    }

    public Path root() {
        return root;
    }

    public Path dataDir() {
        return root.resolve("data");
    }

    public Path scriptsDir() {
        return root.resolve("scripts");
    }

    public Path settingsDir() {
        return root.resolve("settings");
    }

    public Path logsDir() {
        return root.resolve("logs");
    }

    public Path projectsFile() {
        return dataDir().resolve("projects.json");
    }

    public Path projectCatalogFile() {
        return dataDir().resolve("project-catalog.json");
    }

    public Path settingsFile() {
        return settingsDir().resolve("theme.json");
    }
}
