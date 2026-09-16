package com.workflowstudio.recorder;

public record RecordingResult(boolean success, String generatedScript, String errorMessage) {
    public static RecordingResult success(String generatedScript) {
        return new RecordingResult(true, generatedScript, "");
    }

    public static RecordingResult failure(String errorMessage) {
        return new RecordingResult(false, "", errorMessage);
    }
}
