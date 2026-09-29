package com.workflowstudio.recorder;

public interface CodegenRecorder {
    RecordingSession startRecording(String project, String submodule, String screenName);

    RecordingResult stopRecording(RecordingSession session);
}
