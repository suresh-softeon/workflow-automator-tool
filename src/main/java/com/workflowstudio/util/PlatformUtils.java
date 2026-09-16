package com.workflowstudio.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlatformUtils {
    private static final Logger log = LoggerFactory.getLogger(PlatformUtils.class);

    public boolean isSystemDarkMode() {
        return false;
    }

    public void openUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            log.warn("Desktop integration not supported; unable to open URL: {}", url);
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException e) {
            log.error("Failed to open URL: {}", url, e);
        }
    }
}
