package com.workflowstudio.model;

import com.workflowstudio.theme.Theme;
import java.util.Objects;

public record ApplicationSettings(Theme theme) {
    public ApplicationSettings {
        Objects.requireNonNull(theme, "theme is required");
    }
}
