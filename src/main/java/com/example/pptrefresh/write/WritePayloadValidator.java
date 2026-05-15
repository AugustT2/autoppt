package com.example.pptrefresh.write;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import org.springframework.stereotype.Component;

@Component
public class WritePayloadValidator {

    public void validate(TaskDefinition task, TaskWritePayload payload, int tableRows, int tableCols) {
        if (payload.getType() != task.getType()) {
            throw new RefreshException(
                    FailureStage.TASK_DTO_VALIDATE,
                    "TYPE_MISMATCH",
                    "写回类型与任务不一致: " + task.getId(),
                    task.getId(),
                    null);
        }
        switch (task.getType()) {
            case text:
                if (payload.getText() == null) {
                    throw validateError(task, "text 不能为空");
                }
                break;
            case table:
                if (payload.getCells() == null) {
                    throw validateError(task, "cells 不能为空");
                }
                if (payload.getCells().size() != tableRows) {
                    throw validateError(
                            task,
                            "cells 行数应为 " + tableRows + "，实际 " + payload.getCells().size());
                }
                for (int r = 0; r < payload.getCells().size(); r++) {
                    if (payload.getCells().get(r).size() != tableCols) {
                        throw validateError(
                                task,
                                "第 " + r + " 行列数应为 " + tableCols + "，实际 "
                                        + payload.getCells().get(r).size());
                    }
                }
                break;
            case chart:
                if (payload.getCategories() == null
                        || payload.getSeriesNames() == null
                        || payload.getSeriesValues() == null) {
                    throw validateError(task, "chart 需要 categories、seriesNames、seriesValues");
                }
                int n = payload.getSeriesNames().size();
                if (payload.getSeriesValues().size() != n) {
                    throw validateError(task, "seriesValues 行数应与 seriesNames 一致");
                }
                break;
            default:
                throw validateError(task, "未知任务类型");
        }
    }

    private RefreshException validateError(TaskDefinition task, String message) {
        return new RefreshException(
                FailureStage.TASK_DTO_VALIDATE, "PAYLOAD_INVALID", message, task.getId(), null);
    }
}
