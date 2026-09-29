package com.workflowstudio.app;

import com.workflowstudio.util.AppPaths;
import javafx.application.Application;

public final class WorkflowStudioMain {
    private WorkflowStudioMain() {
    }

    public static void main(String[] args) {
        AppPaths.resolve();
        Application.launch(ApplicationLauncher.class, args);
    }
}
