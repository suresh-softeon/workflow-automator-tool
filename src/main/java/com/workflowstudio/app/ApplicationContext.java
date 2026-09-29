package com.workflowstudio.app;

import com.workflowstudio.service.SettingsService;
import com.workflowstudio.service.WorkflowService;
import com.workflowstudio.service.ProjectCatalogService;
import com.workflowstudio.service.RecordingService;
import com.workflowstudio.service.ExecutionService;
import com.workflowstudio.recorder.CodegenRecorder;
import com.workflowstudio.recorder.PlaywrightCodegenRecorder;
import com.workflowstudio.playback.PlaywrightScriptExecutor;
import com.workflowstudio.playback.ScriptExecutor;
import com.workflowstudio.service.impl.DefaultProjectCatalogService;
import com.workflowstudio.service.impl.DefaultRecordingService;
import com.workflowstudio.service.impl.DefaultExecutionService;
import com.workflowstudio.service.impl.DefaultSettingsService;
import com.workflowstudio.service.impl.DefaultWorkflowService;
import com.workflowstudio.storage.FileScriptRepository;
import com.workflowstudio.storage.JsonProjectCatalogRepository;
import com.workflowstudio.storage.JsonSettingsRepository;
import com.workflowstudio.storage.JsonWorkflowRepository;
import com.workflowstudio.storage.ProjectCatalogRepository;
import com.workflowstudio.storage.ScriptRepository;
import com.workflowstudio.storage.SettingsRepository;
import com.workflowstudio.storage.WorkflowRepository;
import com.workflowstudio.theme.ThemeManager;
import com.workflowstudio.util.AppPaths;
import com.workflowstudio.util.DefaultProcessExecutor;
import com.workflowstudio.util.PlatformUtils;
import com.workflowstudio.util.ProcessExecutor;
import com.workflowstudio.validation.NodeValidator;
import com.workflowstudio.validation.PlaywrightValidator;
import com.workflowstudio.validation.StartupValidator;
import com.workflowstudio.validation.WorkflowValidator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ApplicationContext {
    private final ExecutorService executorService;
    private final AppPaths appPaths;
    private final PlatformUtils platformUtils;
    private final ProcessExecutor processExecutor;
    private final WorkflowRepository workflowRepository;
    private final SettingsRepository settingsRepository;
    private final ScriptRepository scriptRepository;
    private final WorkflowValidator workflowValidator;
    private final WorkflowService workflowService;
    private final ProjectCatalogService projectCatalogService;
    private final RecordingService recordingService;
    private final ExecutionService executionService;
    private final SettingsService settingsService;
    private final StartupValidator startupValidator;
    private final ThemeManager themeManager;

    private ApplicationContext(
            ExecutorService executorService,
            AppPaths appPaths,
            PlatformUtils platformUtils,
            ProcessExecutor processExecutor,
            WorkflowRepository workflowRepository,
            SettingsRepository settingsRepository,
            ScriptRepository scriptRepository,
            WorkflowValidator workflowValidator,
            WorkflowService workflowService,
            ProjectCatalogService projectCatalogService,
            RecordingService recordingService,
            ExecutionService executionService,
            SettingsService settingsService,
            StartupValidator startupValidator,
            ThemeManager themeManager
    ) {
        this.executorService = executorService;
        this.appPaths = appPaths;
        this.platformUtils = platformUtils;
        this.processExecutor = processExecutor;
        this.workflowRepository = workflowRepository;
        this.settingsRepository = settingsRepository;
        this.scriptRepository = scriptRepository;
        this.workflowValidator = workflowValidator;
        this.workflowService = workflowService;
        this.projectCatalogService = projectCatalogService;
        this.recordingService = recordingService;
        this.executionService = executionService;
        this.settingsService = settingsService;
        this.startupValidator = startupValidator;
        this.themeManager = themeManager;
    }

    public static ApplicationContext create() {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        AppPaths appPaths = AppPaths.resolve();
        PlatformUtils platformUtils = new PlatformUtils();
        ProcessExecutor processExecutor = new DefaultProcessExecutor();
        WorkflowRepository workflowRepository = new JsonWorkflowRepository(appPaths);
        ProjectCatalogRepository projectCatalogRepository = new JsonProjectCatalogRepository(appPaths);
        SettingsRepository settingsRepository = new JsonSettingsRepository(appPaths);
        ScriptRepository scriptRepository = new FileScriptRepository(appPaths);
        WorkflowValidator workflowValidator = new WorkflowValidator(workflowRepository);
        WorkflowService workflowService = new DefaultWorkflowService(workflowRepository, scriptRepository, workflowValidator);
        ProjectCatalogService projectCatalogService = new DefaultProjectCatalogService(projectCatalogRepository);
        CodegenRecorder codegenRecorder = new PlaywrightCodegenRecorder(appPaths);
        RecordingService recordingService = new DefaultRecordingService(codegenRecorder);
        ScriptExecutor scriptExecutor = new PlaywrightScriptExecutor(appPaths);
        ExecutionService executionService = new DefaultExecutionService(
                workflowRepository,
                scriptExecutor,
                appPaths,
                executor
        );
        SettingsService settingsService = new DefaultSettingsService(settingsRepository);
        StartupValidator startupValidator = new StartupValidator(
                new NodeValidator(processExecutor),
                new PlaywrightValidator(processExecutor)
        );
        ThemeManager themeManager = new ThemeManager(platformUtils);

        return new ApplicationContext(
                executor,
                appPaths,
                platformUtils,
                processExecutor,
                workflowRepository,
                settingsRepository,
                scriptRepository,
                workflowValidator,
                workflowService,
                projectCatalogService,
                recordingService,
                executionService,
                settingsService,
                startupValidator,
                themeManager
        );
    }

    public ExecutorService executorService() {
        return executorService;
    }

    public AppPaths appPaths() {
        return appPaths;
    }

    public PlatformUtils platformUtils() {
        return platformUtils;
    }

    public ProcessExecutor processExecutor() {
        return processExecutor;
    }

    public WorkflowService workflowService() {
        return workflowService;
    }

    public SettingsService settingsService() {
        return settingsService;
    }

    public ProjectCatalogService projectCatalogService() {
        return projectCatalogService;
    }

    public RecordingService recordingService() {
        return recordingService;
    }

    public ExecutionService executionService() {
        return executionService;
    }

    public StartupValidator startupValidator() {
        return startupValidator;
    }

    public ThemeManager themeManager() {
        return themeManager;
    }

    public void shutdown() {
        processExecutor.close();
        executorService.shutdownNow();
    }
}
