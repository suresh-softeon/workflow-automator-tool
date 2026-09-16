package com.workflowstudio.service;

import java.util.List;

public interface ProjectCatalogService {
    List<String> listProjects();

    List<String> listSubmodules(String project);

    void addProject(String project);

    void deleteProject(String project);

    void addSubmodule(String project, String submodule);

    void deleteSubmodule(String project, String submodule);
}
