package com.example.pptrefresh.llm;

import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import com.example.pptrefresh.write.TaskWritePayload;

import java.util.ArrayList;
import java.util.List;

/**
 * 未启用 LLM 时使用：根据模板维度生成占位数据，便于联调 POI 写回链路。
 */
public class StubLlmTaskRunner implements LlmTaskRunner {

    @Override
    public TaskWritePayload fetch(TaskContext context) {
        TaskDefinition task = context.task();
        TaskWritePayload payload = new TaskWritePayload();
        payload.setType(task.getType());
        switch (task.getType()) {
            case text:
                payload.setText(
                        "【占位】"
                                + context.productDisplayName()
                                + "|"
                                + context.fundCode()
                                + "｜"
                                + context.timeContext().latestQuarter()
                                + "｜"
                                + task.getIntent());
                break;
            case table:
                payload.setCells(buildTableCells(context));
                break;
            case chart:
                if ("allocation_chart".equals(task.getId())) {
                    payload.setCategories(
                            List.of("股票", "可转债", "利率债", "信用债"));
                    payload.setSeriesNames(
                            List.of("2025Q2", "2025Q3", "2025Q4", "2026Q1"));
                    payload.setSeriesValues(
                            List.of(
                                    List.of(29.0, 7.0, 23.0, 24.0),
                                    List.of(35.0, 2.0, 20.0, 15.0),
                                    List.of(38.0, 1.0, 20.0, 22.0),
                                    List.of(32.0, 0.0, 12.0, 48.0)));
                } else if ("nav_chart".equals(task.getId())) {
                    payload.setCategories(
                            List.of(
                                    "2024-01",
                                    "2024-07",
                                    "2025-01",
                                    "2025-07",
                                    "2026-01",
                                    "2026-04"));
                    payload.setSeriesNames(
                            List.of("中欧瑾添A", "万得混合债券型二级指数"));
                    payload.setSeriesValues(
                            List.of(
                                    List.of(0.0, -2.0, 2.0, 8.0, 10.0, 12.5),
                                    List.of(2.0, 4.0, 6.0, 10.0, 14.0, 16.0)));
                } else {
                    payload.setCategories(List.of("2024Q2", "2024Q3", "2024Q4", "2025Q1"));
                    payload.setSeriesNames(List.of("系列A", "系列B"));
                    payload.setSeriesValues(
                            List.of(
                                    List.of(10.0, 12.0, 11.0, 13.0),
                                    List.of(5.0, 6.0, 7.0, 8.0)));
                }
                break;
            default:
                break;
        }
        return payload;
    }

    private List<List<String>> buildTableCells(TaskContext context) {
        int rows = context.structure() != null ? context.structure().tableRows() : 3;
        int cols = context.structure() != null ? context.structure().tableCols() : 4;
        List<List<String>> cells = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < cols; c++) {
                row.add(r == 0 ? "列" + (c + 1) : "R" + r + "C" + c);
            }
            cells.add(row);
        }
        return cells;
    }
}
