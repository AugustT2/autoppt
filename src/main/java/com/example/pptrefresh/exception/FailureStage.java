package com.example.pptrefresh.exception;

public enum FailureStage {
    FILENAME_PARSE,
    WHITELIST,
    RULES_LOAD,
    RULES_SCHEMA,
    PRODUCT_NAME_RESOLVE,
    DECK_OPEN,
    TASK_LLM,
    TASK_TOOL,
    TASK_DTO_VALIDATE,
    TASK_RESOLVE_TARGET,
    TASK_WRITE,
    DECK_SAVE,
    UNKNOWN
}
