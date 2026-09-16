package com.workflowstudio.storage;

import java.util.Map;
import java.util.Set;

public interface ProjectCatalogRepository {
    Map<String, Set<String>> load();

    void save(Map<String, Set<String>> catalog);
}
