package com.workflowstudio.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workflowstudio.model.WorkflowMetadata;
import com.workflowstudio.util.AppPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonWorkflowRepository implements WorkflowRepository {
    private static final Logger log = LoggerFactory.getLogger(JsonWorkflowRepository.class);
    private static final TypeReference<List<WorkflowMetadata>> LIST_TYPE = new TypeReference<>() {
    };

    private final AppPaths appPaths;
    private final ObjectMapper objectMapper;
    private final ReentrantLock lock = new ReentrantLock();

    public JsonWorkflowRepository(AppPaths appPaths) {
        this.appPaths = appPaths;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public List<WorkflowMetadata> listAll() {
        lock.lock();
        try {
            ensureFileExists();
            byte[] content = Files.readAllBytes(appPaths.projectsFile());
            if (content.length == 0) {
                return List.of();
            }
            return objectMapper.readValue(content, LIST_TYPE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read workflow metadata.", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<WorkflowMetadata> find(String project, String submodule, String screenName) {
        return listAll().stream()
                .filter(it -> it.project().equals(project)
                        && it.submodule().equals(submodule)
                        && it.screenName().equals(screenName))
                .findFirst();
    }

    @Override
    public void upsert(WorkflowMetadata metadata) {
        lock.lock();
        try {
            List<WorkflowMetadata> workflows = new ArrayList<>(listAll());
            workflows.removeIf(it -> it.project().equals(metadata.project())
                    && it.submodule().equals(metadata.submodule())
                    && it.screenName().equals(metadata.screenName()));
            workflows.add(metadata);
            writeAtomically(workflows);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean delete(String project, String submodule, String screenName) {
        lock.lock();
        try {
            List<WorkflowMetadata> workflows = new ArrayList<>(listAll());
            boolean removed = workflows.removeIf(it -> it.project().equals(project)
                    && it.submodule().equals(submodule)
                    && it.screenName().equals(screenName));
            if (removed) {
                writeAtomically(workflows);
            }
            return removed;
        } finally {
            lock.unlock();
        }
    }

    private void ensureFileExists() throws IOException {
        Files.createDirectories(appPaths.dataDir());
        Path file = appPaths.projectsFile();
        if (!Files.exists(file)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), List.of());
            log.info("Created workflow metadata file at {}", file);
        }
    }

    private void writeAtomically(List<WorkflowMetadata> workflows) {
        try {
            Path target = appPaths.projectsFile();
            Path temp = target.resolveSibling("projects.json.tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), workflows);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write workflow metadata.", e);
        }
    }
}
