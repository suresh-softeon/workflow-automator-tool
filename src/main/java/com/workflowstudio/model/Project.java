package com.workflowstudio.model;

import java.util.List;
import java.util.Objects;

public record Project(String name, List<Submodule> submodules) {
    public Project {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(submodules, "submodules is required");
    }
}
