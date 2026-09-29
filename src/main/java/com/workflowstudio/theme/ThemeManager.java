package com.workflowstudio.theme;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.workflowstudio.util.PlatformUtils;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.Scene;

public final class ThemeManager {
    private static final String APP_STYLESHEET = "/com/workflowstudio/style/workflow-studio.css";

    private final PlatformUtils platformUtils;

    public ThemeManager(PlatformUtils platformUtils) {
        this.platformUtils = Objects.requireNonNull(platformUtils);
    }

    public void applyTheme(Scene scene, Theme requestedTheme) {
        Theme effectiveTheme = requestedTheme == Theme.SYSTEM
                ? (platformUtils.isSystemDarkMode() ? Theme.DARK : Theme.LIGHT)
                : requestedTheme;

        if (effectiveTheme == Theme.DARK) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }

        String appCss = Objects.requireNonNull(getClass().getResource(APP_STYLESHEET)).toExternalForm();
        if (!scene.getStylesheets().contains(appCss)) {
            scene.getStylesheets().add(appCss);
        }
    }
}
