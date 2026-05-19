package com.example.pptrefresh.llm;

import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.sample.ZhongOuSampleData;
import com.example.pptrefresh.write.TaskWritePayload;

import java.util.ArrayList;
import java.util.List;

/** 未启用 LLM 时使用：返回与偏债混样例一致的演示数据，便于联调 POI 写回（仅换数据、保留样式）。 */
public class StubLlmTaskRunner implements LlmTaskRunner {

    @Override
    public TaskWritePayload fetch(TaskContext context) {
        TaskDefinition task = context.task();
        TaskWritePayload payload = new TaskWritePayload();
        payload.setType(task.getType());
        switch (task.getType()) {
            case text:
                payload.setText(stubText(context, task));
                break;
            case table:
                if ("yield_ranking_table".equals(task.getId())) {
                    payload.setCells(copyCells(ZhongOuSampleData.YIELD_RANKING_CELLS));
                } else {
                    payload.setCells(buildPlaceholderTable(context));
                }
                break;
            case chart:
                stubChart(payload, task);
                break;
            default:
                break;
        }
        return payload;
    }

    private static String stubText(TaskContext context, TaskDefinition task) {
        String name =
                context.productDisplayName().isBlank()
                        ? "蓝海稳健增长混合A"
                        : context.productDisplayName();
        switch (task.getId()) {
            case "title":
                return "基金业绩说明（" + name + "）";
            case "fund_meta":
                return "偏股混合型基金\n"
                        + "成立日期：2019-06-12　　最新规模：58.6 亿元（示例）\n"
                        + "基金经理：张明、李悦（示例）\n"
                        + "业绩比较基准：沪深300指数收益率×70% + 中债综合指数×30%\n"
                        + "风险等级：R3（中风险）　　托管人：示例商业银行";
            default:
                return "【stub】"
                        + name
                        + " "
                        + context.fundCode()
                        + " "
                        + context.timeContext().latestQuarter();
        }
    }

    private static void stubChart(TaskWritePayload payload, TaskDefinition task) {
        if ("allocation_chart".equals(task.getId())) {
            payload.setCategories(ZhongOuSampleData.ALLOCATION_CATEGORIES);
            payload.setSeriesNames(ZhongOuSampleData.ALLOCATION_SERIES_NAMES);
            payload.setSeriesValues(ZhongOuSampleData.ALLOCATION_SERIES_VALUES);
        } else if ("nav_chart".equals(task.getId())) {
            payload.setCategories(
                    List.of(
                            "2024-05",
                            "2024-07",
                            "2024-09",
                            "2024-11",
                            "2025-01",
                            "2025-03",
                            "2025-05"));
            payload.setSeriesNames(List.of("本基金", "业绩基准"));
            payload.setSeriesValues(
                    List.of(
                            List.of(0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0),
                            List.of(0.0, 1.5, 3.0, 4.5, 6.0, 7.5, 9.0)));
        } else {
            payload.setCategories(List.of("2024Q2", "2024Q3", "2024Q4", "2025Q1"));
            payload.setSeriesNames(List.of("系列A", "系列B"));
            payload.setSeriesValues(
                    List.of(
                            List.of(10.0, 12.0, 11.0, 13.0),
                            List.of(5.0, 6.0, 7.0, 8.0)));
        }
    }

    private static List<List<String>> copyCells(List<List<String>> source) {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> row : source) {
            copy.add(new ArrayList<>(row));
        }
        return copy;
    }

    private static List<List<String>> buildPlaceholderTable(TaskContext context) {
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
