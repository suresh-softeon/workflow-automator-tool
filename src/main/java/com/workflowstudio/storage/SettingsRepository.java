package com.workflowstudio.storage;

import com.workflowstudio.model.ApplicationSettings;

public interface SettingsRepository {
    ApplicationSettings load();

    void save(ApplicationSettings settings);
}
