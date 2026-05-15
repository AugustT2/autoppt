package com.example.pptrefresh.llm;

import com.example.pptrefresh.write.TaskWritePayload;

public interface LlmTaskRunner {

    TaskWritePayload fetch(TaskContext context);
}
