package com.workflowstudio.service.impl;

import com.workflowstudio.service.ProjectCatalogService;
import com.workflowstudio.storage.ProjectCatalogRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DefaultProjectCatalogService implements ProjectCatalogService {
    private final ProjectCatalogRepository repository;

    public DefaultProjectCatalogService(ProjectCatalogRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public List<String> listProjects() {
        return repository.load().keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    @Override
    public List<String> listSubmodules(String project) {
        String projectName = normalize(project, "Project");
        Set<String> submodules = repository.load().get(projectName);
        if (submodules == null) {
            return List.of();
        }
        return submodules.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    @Override
    public void addProject(String project) {
        String projectName = normalize(project, "Project");
        Map<String, Set<String>> catalog = repository.load();
        if (catalog.containsKey(projectName)) {
            throw new IllegalArgumentException("Project already exists.");
        }
        catalog.put(projectName, new LinkedHashSet<>());
        repository.save(catalog);
    }

    @Override
    public void deleteProject(String project) {
        String projectName = normalize(project, "Project");
        Map<String, Set<String>> catalog = repository.load();
        if (catalog.remove(projectName) == null) {
            throw new IllegalArgumentException("Project does not exist.");
        }
        repository.save(catalog);
    }

    @Override
    public void addSubmodule(String project, String submodule) {
        String projectName = normalize(project, "Project");
        String submoduleName = normalize(submodule, "Submodule");
        Map<String, Set<String>> catalog = repository.load();
        Set<String> submodules = catalog.get(projectName);
        if (submodules == null) {
            throw new IllegalArgumentException("Select a valid project first.");
        }
        if (!submodules.add(submoduleName)) {
            throw new IllegalArgumentException("Submodule already exists in this project.");
        }
        repository.save(catalog);
    }

    @Override
    public void deleteSubmodule(String project, String submodule) {
        String projectName = normalize(project, "Project");
        String submoduleName = normalize(submodule, "Submodule");
        Map<String, Set<String>> catalog = repository.load();
        Set<String> submodules = catalog.get(projectName);
        if (submodules == null) {
            throw new IllegalArgumentException("Select a valid project first.");
        }
        if (!submodules.remove(submoduleName)) {
            throw new IllegalArgumentException("Submodule does not exist in this project.");
        }
        repository.save(catalog);
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " name is required.");
        }
        return value.trim();
    }
}
