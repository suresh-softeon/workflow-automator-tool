package com.workflowstudio.service;

import com.workflowstudio.service.impl.DefaultProjectCatalogService;
import com.workflowstudio.storage.ProjectCatalogRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultProjectCatalogServiceTest {
    @Test
    void addsAndDeletesProjectAndSubmodule() {
        InMemoryCatalogRepository repository = new InMemoryCatalogRepository();
        DefaultProjectCatalogService service = new DefaultProjectCatalogService(repository);

        service.addProject("UMG");
        service.addSubmodule("UMG", "Orders");
        service.addSubmodule("UMG", "Shipping");

        Assertions.assertEquals(List.of("UMG"), service.listProjects());
        Assertions.assertEquals(List.of("Orders", "Shipping"), service.listSubmodules("UMG"));

        service.deleteSubmodule("UMG", "Orders");
        Assertions.assertEquals(List.of("Shipping"), service.listSubmodules("UMG"));

        service.deleteProject("UMG");
        Assertions.assertTrue(service.listProjects().isEmpty());
    }

    @Test
    void rejectsInvalidInputs() {
        InMemoryCatalogRepository repository = new InMemoryCatalogRepository();
        DefaultProjectCatalogService service = new DefaultProjectCatalogService(repository);

        Assertions.assertThrows(IllegalArgumentException.class, () -> service.addProject(" "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.addSubmodule("UMG", "Orders"));
    }

    private static final class InMemoryCatalogRepository implements ProjectCatalogRepository {
        private Map<String, Set<String>> catalog = new LinkedHashMap<>();

        @Override
        public Map<String, Set<String>> load() {
            Map<String, Set<String>> copy = new LinkedHashMap<>();
            catalog.forEach((k, v) -> copy.put(k, new LinkedHashSet<>(v)));
            return copy;
        }

        @Override
        public void save(Map<String, Set<String>> catalog) {
            this.catalog = new LinkedHashMap<>();
            catalog.forEach((k, v) -> this.catalog.put(k, new LinkedHashSet<>(v)));
        }
    }
}
