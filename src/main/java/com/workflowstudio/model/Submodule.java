package com.workflowstudio.model;

import java.util.List;
import java.util.Objects;

public record Submodule(String name, List<Screen> screens) {
    public Submodule {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(screens, "screens is required");
    }
}
