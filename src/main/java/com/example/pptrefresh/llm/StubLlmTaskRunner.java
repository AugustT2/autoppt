package com.example.pptrefresh.llm;

import com.example.pptrefresh.funds.FundFactsClient;
import com.example.pptrefresh.query.ChartSeriesData;
import com.example.pptrefresh.query.QueryPlan;
import com.example.pptrefresh.query.QueryPlanDataService;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TextReplaceMode;
import com.example.pptrefresh.sample.ZhongOuSampleData;
import com.example.pptrefresh.write.TaskWritePayload;

import java.util.ArrayList;
import java.util.List;

/** 未启用 LLM 时使用：返回与偏债混样例一致的演示数据，便于联调 POI 写回（仅换数据、保留样式）。 */
public class StubLlmTaskRunner implements LlmTaskRunner {

    private final FundFactsClient fundFactsClient;
    private final QueryPlanDataService queryPlanDataService;

    public StubLlmTaskRunner(FundFactsClient fundFactsClient, QueryPlanDataService queryPlanDataService) {
        this.fundFactsClient = fundFactsClient;
        this.queryPlanDataService = queryPlanDataService;
    }

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
                if (context.queryPlan() != null) {
                    int rows =
                            context.structure() != null ? context.structure().tableRows() : 7;
                    int cols =
                            context.structure() != null ? context.structure().tableCols() : 4;
                    payload.setCells(
                            queryPlanDataService.buildTableCells(
                                    context.queryPlan(), context.fundCode(), rows, cols));
                } else if ("yield_ranking_table".equals(task.getId())) {
                    payload.setCells(copyCells(ZhongOuSampleData.YIELD_RANKING_CELLS));
                } else {
                    payload.setCells(buildPlaceholderTable(context));
                }
                break;
            case chart:
                stubChart(payload, task, context);
                break;
            default:
                break;
        }
        return payload;
    }

    private String stubText(TaskContext context, TaskDefinition task) {
        if (task.getMode() == TextReplaceMode.replace_labeled_number) {
            return fundFactsClient
                    .fetchLatestScale(context.fundCode())
                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "无基金规模数据: fundCode=" + context.fundCode()));
        }
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

    private void stubChart(TaskWritePayload payload, TaskDefinition task, TaskContext context) {
        QueryPlan queryPlan = context.queryPlan();
        if (queryPlan != null && "allocation_chart".equals(task.getId())) {
            ChartSeriesData data =
                    queryPlanDataService.buildAllocationChart(queryPlan, context.fundCode());
            applyChart(payload, data);
            return;
        }
        if (queryPlan != null && "nav_chart".equals(task.getId())) {
            ChartSeriesData data =
                    queryPlanDataService.buildNavChart(
                            queryPlan, context.fundCode(), "业绩基准");
            applyChart(payload, data);
            return;
        }
        List<String> categoriesFromPlan =
                queryPlan != null && queryPlan.writeBack() != null
                        ? queryPlan.writeBack().categoryLabels()
                        : null;
        if ("allocation_chart".equals(task.getId())) {
            List<String> categories =
                    categoriesFromPlan != null && !categoriesFromPlan.isEmpty()
                            ? categoriesFromPlan
                            : ZhongOuSampleData.ALLOCATION_CATEGORIES;
            payload.setCategories(categories);
            payload.setSeriesNames(ZhongOuSampleData.ALLOCATION_SERIES_NAMES);
            payload.setSeriesValues(ZhongOuSampleData.ALLOCATION_SERIES_VALUES);
        } else if ("nav_chart".equals(task.getId())) {
            List<String> categories =
                    categoriesFromPlan != null && !categoriesFromPlan.isEmpty()
                            ? categoriesFromPlan
                            : List.of(
                                    "2024-05",
                                    "2024-07",
                                    "2024-09",
                                    "2024-11",
                                    "2025-01",
                                    "2025-03",
                                    "2025-05");
            int n = categories.size();
            payload.setCategories(categories);
            payload.setSeriesNames(List.of("本基金", "业绩基准"));
            payload.setSeriesValues(
                    List.of(
                            linearSeries(0.0, 12.0, n),
                            linearSeries(0.0, 9.0, n)));
        } else {
            payload.setCategories(
                    categoriesFromPlan != null && !categoriesFromPlan.isEmpty()
                            ? categoriesFromPlan
                            : List.of("2024Q2", "2024Q3", "2024Q4", "2025Q1"));
            payload.setSeriesNames(List.of("系列A", "系列B"));
            payload.setSeriesValues(
                    List.of(
                            List.of(10.0, 12.0, 11.0, 13.0),
                            List.of(5.0, 6.0, 7.0, 8.0)));
        }
    }

    private static void applyChart(TaskWritePayload payload, ChartSeriesData data) {
        payload.setCategories(data.categories());
        payload.setSeriesNames(data.seriesNames());
        payload.setSeriesValues(data.seriesValues());
    }

    private static List<Double> linearSeries(double start, double end, int points) {
        List<Double> values = new ArrayList<>(points);
        if (points <= 1) {
            values.add(end);
            return values;
        }
        double step = (end - start) / (points - 1);
        for (int i = 0; i < points; i++) {
            values.add(start + step * i);
        }
        return values;
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
