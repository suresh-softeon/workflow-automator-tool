package com.workflowstudio.theme;

public enum Theme {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System");

    private final String label;

    Theme(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
