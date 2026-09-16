package com.workflowstudio.service;

import com.workflowstudio.model.ApplicationSettings;
import com.workflowstudio.theme.Theme;

public interface SettingsService {
    ApplicationSettings load();

    void saveTheme(Theme theme);
}
