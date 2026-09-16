package com.workflowstudio.service.impl;

import com.workflowstudio.recorder.CodegenRecorder;
import com.workflowstudio.recorder.RecordingResult;
import com.workflowstudio.recorder.RecordingSession;
import com.workflowstudio.service.RecordingService;
import java.util.Objects;

public final class DefaultRecordingService implements RecordingService {
    private final CodegenRecorder codegenRecorder;

    public DefaultRecordingService(CodegenRecorder codegenRecorder) {
        this.codegenRecorder = Objects.requireNonNull(codegenRecorder);
    }

    @Override
    public RecordingSession startRecording(String project, String submodule, String screenName) {
        return codegenRecorder.startRecording(project, submodule, screenName);
    }

    @Override
    public RecordingResult stopRecording(RecordingSession session) {
        return codegenRecorder.stopRecording(session);
    }
}
