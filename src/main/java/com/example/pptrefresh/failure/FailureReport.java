package com.example.pptrefresh.failure;

import com.example.pptrefresh.exception.FailureStage;

public class FailureReport {

    private String version = "1";
    private String failedAt;
    private String traceId;
    private String jobId;
    private String deckType;
    private String sourceFile;
    private String expectedOutputFile;
    private String productName;
    private String fundCode;
    private FailureStage stage;
    private String taskId;
    private Integer slideIndex;
    private String errorCode;
    private String message;
    private String cause;
    private String toolName;
    private String llmRequestId;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(String failedAt) {
        this.failedAt = failedAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getDeckType() {
        return deckType;
    }

    public void setDeckType(String deckType) {
        this.deckType = deckType;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getExpectedOutputFile() {
        return expectedOutputFile;
    }

    public void setExpectedOutputFile(String expectedOutputFile) {
        this.expectedOutputFile = expectedOutputFile;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public FailureStage getStage() {
        return stage;
    }

    public void setStage(FailureStage stage) {
        this.stage = stage;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Integer getSlideIndex() {
        return slideIndex;
    }

    public void setSlideIndex(Integer slideIndex) {
        this.slideIndex = slideIndex;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getLlmRequestId() {
        return llmRequestId;
    }

    public void setLlmRequestId(String llmRequestId) {
        this.llmRequestId = llmRequestId;
    }
}
