package com.workflowstudio.model;

import java.util.Objects;

public record Screen(String name) {
    public Screen {
        Objects.requireNonNull(name, "name is required");
    }
}
