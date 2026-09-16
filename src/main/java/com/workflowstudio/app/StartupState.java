package com.workflowstudio.app;

import com.workflowstudio.model.ApplicationSettings;
import com.workflowstudio.theme.Theme;
import com.workflowstudio.validation.ValidationResult;

public record StartupState(ApplicationSettings settings, ValidationResult validation) {
    public static StartupState success(ApplicationSettings settings) {
        return new StartupState(settings, ValidationResult.valid());
    }

    public static StartupState failed(ValidationResult validation) {
        return new StartupState(new ApplicationSettings(Theme.SYSTEM), validation);
    }
}
