package com.workflowstudio.controller;

import java.util.Objects;

public final class HomeController {
    private final Runnable openRecorderAction;
    private final Runnable openProjectsAction;

    public HomeController(Runnable openRecorderAction, Runnable openProjectsAction) {
        this.openRecorderAction = Objects.requireNonNull(openRecorderAction);
        this.openProjectsAction = Objects.requireNonNull(openProjectsAction);
    }

    public void openRecorder() {
        openRecorderAction.run();
    }

    public void openProjects() {
        openProjectsAction.run();
    }
}
