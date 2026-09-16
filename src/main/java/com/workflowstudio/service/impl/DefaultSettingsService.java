package com.workflowstudio.service.impl;

import com.workflowstudio.model.ApplicationSettings;
import com.workflowstudio.service.SettingsService;
import com.workflowstudio.storage.SettingsRepository;
import com.workflowstudio.theme.Theme;
import java.util.Objects;

public final class DefaultSettingsService implements SettingsService {
    private final SettingsRepository settingsRepository;

    public DefaultSettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
    }

    @Override
    public ApplicationSettings load() {
        return settingsRepository.load();
    }

    @Override
    public void saveTheme(Theme theme) {
        settingsRepository.save(new ApplicationSettings(theme));
    }
}
