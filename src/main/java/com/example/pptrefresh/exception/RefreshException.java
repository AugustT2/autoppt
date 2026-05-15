package com.example.pptrefresh.exception;

public class RefreshException extends RuntimeException {

    private final FailureStage stage;
    private final String errorCode;
    private final String taskId;

    public RefreshException(FailureStage stage, String errorCode, String message) {
        this(stage, errorCode, message, null, null);
    }

    public RefreshException(
            FailureStage stage, String errorCode, String message, String taskId, Throwable cause) {
        super(message, cause);
        this.stage = stage;
        this.errorCode = errorCode;
        this.taskId = taskId;
    }

    public FailureStage getStage() {
        return stage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTaskId() {
        return taskId;
    }
}
