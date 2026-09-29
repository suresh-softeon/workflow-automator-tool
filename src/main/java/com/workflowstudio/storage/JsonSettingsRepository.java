package com.workflowstudio.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowstudio.model.ApplicationSettings;
import com.workflowstudio.theme.Theme;
import com.workflowstudio.util.AppPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class JsonSettingsRepository implements SettingsRepository {
    private final AppPaths appPaths;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonSettingsRepository(AppPaths appPaths) {
        this.appPaths = appPaths;
    }

    @Override
    public ApplicationSettings load() {
        try {
            ensureFileExists();
            Map<?, ?> raw = objectMapper.readValue(appPaths.settingsFile().toFile(), Map.class);
            Object themeName = raw.get("theme");
            Theme theme = parseTheme(themeName);
            return new ApplicationSettings(theme);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load settings.", e);
        }
    }

    @Override
    public void save(ApplicationSettings settings) {
        try {
            Path target = appPaths.settingsFile();
            Path temp = target.resolveSibling("theme.json.tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), Map.of("theme", settings.theme().name()));
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save settings.", e);
        }
    }

    private void ensureFileExists() throws IOException {
        Files.createDirectories(appPaths.settingsDir());
        Path file = appPaths.settingsFile();
        if (!Files.exists(file)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), Map.of("theme", Theme.SYSTEM.name()));
        }
    }

    private Theme parseTheme(Object themeName) {
        if (themeName == null) {
            return Theme.SYSTEM;
        }
        try {
            return Theme.valueOf(themeName.toString());
        } catch (IllegalArgumentException ex) {
            return Theme.SYSTEM;
        }
    }
}
