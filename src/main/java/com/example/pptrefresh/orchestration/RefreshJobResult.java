package com.example.pptrefresh.orchestration;

public final class RefreshJobResult {

    private final boolean success;
    private final String jobId;
    private final String outputPptxPath;
    private final String failedJsonPath;
    private final String message;

    public RefreshJobResult(
            boolean success,
            String jobId,
            String outputPptxPath,
            String failedJsonPath,
            String message) {
        this.success = success;
        this.jobId = jobId;
        this.outputPptxPath = outputPptxPath;
        this.failedJsonPath = failedJsonPath;
        this.message = message;
    }

    public boolean success() {
        return success;
    }

    public String jobId() {
        return jobId;
    }

    public String outputPptxPath() {
        return outputPptxPath;
    }

    public String failedJsonPath() {
        return failedJsonPath;
    }

    public String message() {
        return message;
    }
}
