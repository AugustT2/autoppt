package com.example.pptrefresh.orchestration;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty("success")
    public boolean isSuccess() {
        return success;
    }

    @JsonProperty("jobId")
    public String getJobId() {
        return jobId;
    }

    @JsonProperty("outputPptxPath")
    public String getOutputPptxPath() {
        return outputPptxPath;
    }

    @JsonProperty("failedJsonPath")
    public String getFailedJsonPath() {
        return failedJsonPath;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }
}
