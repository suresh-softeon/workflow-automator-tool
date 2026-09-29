package com.workflowstudio.service;

import com.workflowstudio.recorder.RecordingResult;
import com.workflowstudio.recorder.RecordingSession;

public interface RecordingService {
    RecordingSession startRecording(String project, String submodule, String screenName);

    RecordingResult stopRecording(RecordingSession session);
}
