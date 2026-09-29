package com.workflowstudio.validation;

public record ValidationResult(
        boolean success,
        String title,
        String message,
        String actionLabel,
        String actionUrl
) {
    public static ValidationResult valid() {
        return new ValidationResult(true, "OK", "Validation succeeded.", "", "");
    }

    public static ValidationResult nodeMissing() {
        return new ValidationResult(
                false,
                "Node.js is required for Workflow Studio.",
                "Please install Node.js LTS and restart the application.",
                "Download Node.js",
                "https://nodejs.org/en/download"
        );
    }

    public static ValidationResult playwrightMissing() {
        return new ValidationResult(
                false,
                "Playwright is required.",
                "Please install Playwright and restart Workflow Studio.",
                "Playwright Installation Guide",
                "https://playwright.dev/docs/intro"
        );
    }

    public static ValidationResult startupFilesystemFailure(Exception exception) {
        return new ValidationResult(
                false,
                "Unable to initialize Workflow Studio.",
                "Required application directories could not be created: " + exception.getMessage(),
                "Open Node.js Website",
                "https://nodejs.org"
        );
    }
}
