package com.workflowstudio.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowstudio.util.AppPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonProjectCatalogRepository implements ProjectCatalogRepository {
    private static final String FILE_NAME = "project-catalog.json";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Logger log = LoggerFactory.getLogger(JsonProjectCatalogRepository.class);

    private final AppPaths appPaths;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantLock lock = new ReentrantLock();

    public JsonProjectCatalogRepository(AppPaths appPaths) {
        this.appPaths = appPaths;
    }

    @Override
    public Map<String, Set<String>> load() {
        lock.lock();
        try {
            ensureFileExists();
            if (Files.size(appPaths.projectCatalogFile()) == 0) {
                initializeEmptyCatalog();
                return new LinkedHashMap<>();
            }

            Map<String, Object> root = objectMapper.readValue(appPaths.projectCatalogFile().toFile(), MAP_TYPE);
            Object projectsNode = root.get("projects");
            Map<String, Set<String>> result = new LinkedHashMap<>();
            if (projectsNode instanceof Map<?, ?> projects) {
                projects.forEach((project, submodules) -> {
                    if (project instanceof String projectName && !projectName.isBlank()) {
                        result.put(projectName, convertSubmodules(submodules));
                    }
                });
            }
            return result;
        } catch (IOException e) {
            log.error("Project catalog is unreadable. Resetting to empty catalog.", e);
            try {
                initializeEmptyCatalog();
                return new LinkedHashMap<>();
            } catch (IOException resetError) {
                throw new IllegalStateException("Unable to load project catalog.", resetError);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void save(Map<String, Set<String>> catalog) {
        lock.lock();
        try {
            ensureFileExists();
            Map<String, List<String>> projects = new LinkedHashMap<>();
            catalog.forEach((project, submodules) -> projects.put(project, new ArrayList<>(submodules)));
            Path target = appPaths.projectCatalogFile();
            Path temp = target.resolveSibling(FILE_NAME + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), Map.of("projects", projects));
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save project catalog.", e);
        } finally {
            lock.unlock();
        }
    }

    private void ensureFileExists() throws IOException {
        Files.createDirectories(appPaths.dataDir());
        Path file = appPaths.projectCatalogFile();
        if (!Files.exists(file)) {
            initializeEmptyCatalog();
        }
    }

    private Set<String> convertSubmodules(Object rawSubmodules) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (!(rawSubmodules instanceof List<?> entries)) {
            return result;
        }
        for (Object entry : entries) {
            if (entry instanceof String name && !name.isBlank()) {
                result.add(name.trim());
            }
        }
        return result;
    }

    private void initializeEmptyCatalog() throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(appPaths.projectCatalogFile().toFile(), Map.of("projects", Map.of()));
    }
}
