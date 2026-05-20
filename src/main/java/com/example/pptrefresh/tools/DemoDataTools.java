package com.example.pptrefresh.tools;

import com.example.pptrefresh.funds.FundFactsClient;
import com.example.pptrefresh.funds.HardcodedFundCodeLookup;
import com.example.pptrefresh.llm.TaskContext;
import com.example.pptrefresh.llm.TaskContextHolder;
import com.example.pptrefresh.query.ChartSeriesData;
import com.example.pptrefresh.query.QueryPlan;
import com.example.pptrefresh.query.QueryPlanDataService;
import com.example.pptrefresh.sample.ZhongOuSampleData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示用数据 Tool：与 {@link com.example.pptrefresh.sample.ZhongOuSampleData} / {@code 偏债混-M1.yaml} 对齐。
 */
@Component
public class DemoDataTools {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HardcodedFundCodeLookup fundCodeLookup;
    private final FundFactsClient fundFactsClient;
    private final QueryPlanDataService queryPlanDataService;

    public DemoDataTools(
            HardcodedFundCodeLookup fundCodeLookup,
            FundFactsClient fundFactsClient,
            QueryPlanDataService queryPlanDataService) {
        this.fundCodeLookup = fundCodeLookup;
        this.fundFactsClient = fundFactsClient;
        this.queryPlanDataService = queryPlanDataService;
    }

    @Tool("根据产品名称查询产品代码")
    public String lookupProductCode(String productName) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            String code = fundCodeLookup.lookupFundCode(productName);
            m.put("productCode", code != null ? code : "");
            m.put("productNameInput", productName);
            m.put("note", code != null ? "hardcoded demo DB" : "no mapping for display name");
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool("查询产品在指定季度的收益率摘要")
    public String fetchQuarterReturnSummary(String productCode, String quarter) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productCode", productCode);
            m.put("quarter", quarter);
            m.put("returnPct", "12.8%");
            m.put("rankPct", "前22%");
            m.put("benchmarkExcess", "+1.2pct");
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool("查询主标题文案（title task）。返回 titleText，写回 text 时整段 replace_all。")
    public String fetchTitleText(
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("产品线前缀，如偏债混；与用户消息 deckType 中 - 前一段一致") String productLinePrefix) {
        try {
            String display =
                    (productName == null || productName.isBlank())
                            ? "蓝海稳健增长混合A"
                            : productName.trim();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("titleText", "基金业绩说明（" + display + "）");
            m.put("productLinePrefix", productLinePrefix);
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool("查询基金基本信息后缀（fund_meta task，after_anchor）。返回 fundMetaAfterAnchor。")
    public String fetchFundMetaAfterAnchor(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("报告截止季度，与用户消息 latestQuarter 一致") String latestQuarter) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productCode", productCode);
            m.put("productName", productName);
            m.put("latestQuarter", latestQuarter);
            m.put(
                    "fundMetaAfterAnchor",
                    "偏股混合型基金\n"
                            + "成立日期：2099-06-12　　最新规模：100 亿元（示例）\n"
                            + "基金经理：张三、李四（示例）\n"
                            + "业绩比较基准：沪深300指数收益率×70% + 中债综合指数×30%\n"
                            + "风险等级：R3（中风险）　　托管人：示例商业银行");
            m.put("note", "demo mock");
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool(
            "查询最新基金规模数值（fund_latest_scale 等 replace_labeled_number 任务）。"
                    + "返回 scaleValue 为仅数字字符串，写回 text 时只用该数字，不含亿元等单位。")
    public String fetchFundLatestScale(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("字段标签，与用户消息 fieldLabel 一致，如 最新规模") String fieldLabel) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productCode", productCode);
            m.put("fieldLabel", fieldLabel);
            String scale =
                    fundFactsClient.fetchLatestScale(productCode).orElse("");
            m.put("scaleValue", scale);
            m.put("note", "demo via FundFactsClient");
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool("查询投资范围及策略正文后缀（strategy task，after_anchor）。返回 strategyAfterAnchor。")
    public String fetchStrategyAfterAnchor(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productCode", productCode);
            m.put("productName", productName);
            m.put(
                    "strategyAfterAnchor",
                    "\n以30%权益中枢的高波收益+策略，在控制波动与回撤的前提下增强收益。\n"
                            + "股票：采用smart beta宏观择时与自下而上宏观敏感度体系。\n"
                            + "可转债：采用估值交易指数体系与量化模型获取可持续alpha。\n"
                            + "纯债：高等级信用债为底仓，辅以利率交易。");
            m.put("note", "demo mock");
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool(
            "查询收益率排名表（yield_ranking_table task）。返回 cells 二维数组。"
                    + "若当前任务上下文含 queryPlan，将按 plan 中每行/列区间条件逐条查数组装 cells。")
    public String fetchPerformanceTable(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("报告截止季度，与用户消息 latestQuarter 一致") String latestQuarter,
            @P("表格行数，与用户消息 dimensions.rows 一致（样例 7）") int tableRows,
            @P("表格列数，与用户消息 dimensions.cols 一致（样例 6）") int tableCols) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productCode", productCode);
            m.put("productName", productName);
            m.put("latestQuarter", latestQuarter);
            m.put("requestedTableRows", tableRows);
            m.put("requestedTableCols", tableCols);
            QueryPlan plan = queryPlanFromContext();
            if (plan != null) {
                m.put(
                        "cells",
                        queryPlanDataService.buildTableCells(plan, productCode, tableRows, tableCols));
                m.put("queryPlanUsed", true);
                m.put("note", "按 queryPlan 逐区间查表（StubQueryPlanDataClient）");
            } else {
                m.put("cells", ZhongOuSampleData.YIELD_RANKING_CELLS);
                m.put("queryPlanUsed", false);
                m.put("note", "无 queryPlan，回退固定样例 cells");
            }
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool(
            "查询大类资产配置分组柱状图数据（allocation_chart）。"
                    + "返回 categories、seriesNames、seriesValues。"
                    + "若上下文含 queryPlan，按各季度 category 逐点查资产配置。")
    public String fetchAllocationChart(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("报告截止季度，与用户消息 latestQuarter 一致") String latestQuarter,
            @P("横轴分类个数（样例 4）") int categoryCount,
            @P("系列个数（样例 3）") int seriesCount,
            @P("可选：queryPlan.writeBack.categoryLabels 的 JSON 数组字符串") String categoryLabelsJson) {
        try {
            Map<String, Object> root;
            QueryPlan plan = queryPlanFromContext();
            if (plan != null) {
                ChartSeriesData data = queryPlanDataService.buildAllocationChart(plan, productCode);
                root = chartMap(data);
                root.put("queryPlanUsed", true);
                root.put("note", "按 queryPlan 逐季度查资产配置（StubQueryPlanDataClient）");
            } else {
                root = new LinkedHashMap<>(mockAllocationChartPayload(categoryLabelsJson));
                root.put("queryPlanUsed", false);
                root.put("note", "无 queryPlan，回退固定样例或 categoryLabelsJson");
            }
            root.put("chartId", "allocation_chart");
            root.put("productCode", productCode);
            root.put("productName", productName);
            root.put("latestQuarter", latestQuarter);
            root.put("requestedCategoryCount", categoryCount);
            root.put("requestedSeriesCount", seriesCount);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool(
            "查询累计收益率折线图数据（nav_chart）。返回 categories、seriesNames、seriesValues。"
                    + "若上下文含 queryPlan，按各月份 category 逐点查累计收益。")
    public String fetchNavChart(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("数据截止日，与用户消息 latestDate 一致") String latestDate,
            @P("业绩基准名称") String benchmarkName,
            @P("横轴分类个数（样例 7）") int categoryCount,
            @P("系列条数（样例 2）") int seriesCount,
            @P("可选：queryPlan.writeBack.categoryLabels 的 JSON 数组字符串") String categoryLabelsJson) {
        try {
            Map<String, Object> root;
            QueryPlan plan = queryPlanFromContext();
            if (plan != null) {
                ChartSeriesData data =
                        queryPlanDataService.buildNavChart(plan, productCode, benchmarkName);
                root = chartMap(data);
                root.put("queryPlanUsed", true);
                root.put("note", "按 queryPlan 逐月份查累计收益（StubQueryPlanDataClient）");
            } else {
                root =
                        new LinkedHashMap<>(
                                mockNavChartPayload(productName, benchmarkName, categoryLabelsJson));
                root.put("queryPlanUsed", false);
                root.put("note", "无 queryPlan，回退固定样例或 categoryLabelsJson");
            }
            root.put("chartId", "nav_chart");
            root.put("productCode", productCode);
            root.put("productName", productName);
            root.put("latestDate", latestDate);
            root.put("benchmarkName", benchmarkName);
            root.put("requestedCategoryCount", categoryCount);
            root.put("requestedSeriesCount", seriesCount);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> mockAllocationChartPayload(String categoryLabelsJson) throws Exception {
        Map<String, Object> chart = new LinkedHashMap<>();
        List<String> categories = parseCategories(categoryLabelsJson);
        if (categories == null || categories.isEmpty()) {
            categories = ZhongOuSampleData.ALLOCATION_CATEGORIES;
        }
        chart.put("categories", categories);
        chart.put("seriesNames", ZhongOuSampleData.ALLOCATION_SERIES_NAMES);
        chart.put("seriesValues", resizeSeries(ZhongOuSampleData.ALLOCATION_SERIES_VALUES, categories.size()));
        return chart;
    }

    private Map<String, Object> mockNavChartPayload(
            String productName, String benchmarkName, String categoryLabelsJson) throws Exception {
        String fundSeries =
                (productName == null || productName.isBlank()) ? "本基金" : "本基金";
        String bench =
                (benchmarkName == null || benchmarkName.isBlank()) ? "业绩基准" : benchmarkName.trim();
        Map<String, Object> chart = new LinkedHashMap<>();
        List<String> categories = parseCategories(categoryLabelsJson);
        if (categories == null || categories.isEmpty()) {
            categories =
                    List.of(
                            "2024-05",
                            "2024-07",
                            "2024-09",
                            "2024-11",
                            "2025-01",
                            "2025-03",
                            "2025-05");
        }
        int n = categories.size();
        chart.put("categories", categories);
        chart.put("seriesNames", List.of(fundSeries, bench));
        chart.put(
                "seriesValues",
                List.of(
                        linear(0.0, 12.0, n),
                        linear(0.0, 9.0, n)));
        return chart;
    }

    private static QueryPlan queryPlanFromContext() {
        TaskContext ctx = TaskContextHolder.get();
        return ctx != null ? ctx.queryPlan() : null;
    }

    private static Map<String, Object> chartMap(ChartSeriesData data) {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("categories", data.categories());
        chart.put("seriesNames", data.seriesNames());
        chart.put("seriesValues", data.seriesValues());
        return chart;
    }

    private List<String> parseCategories(String categoryLabelsJson) throws Exception {
        if (!StringUtils.hasText(categoryLabelsJson)) {
            return null;
        }
        return mapper.readValue(categoryLabelsJson, new TypeReference<List<String>>() {});
    }

    private static List<List<Double>> resizeSeries(List<List<Double>> source, int size) {
        List<List<Double>> out = new ArrayList<>();
        for (List<Double> series : source) {
            out.add(linear(series.isEmpty() ? 0.0 : series.get(0), series.get(series.size() - 1), size));
        }
        return out;
    }

    private static List<Double> linear(double start, double end, int points) {
        List<Double> values = new ArrayList<>();
        if (points <= 0) {
            return values;
        }
        if (points == 1) {
            values.add(end);
            return values;
        }
        double step = (end - start) / (points - 1);
        for (int i = 0; i < points; i++) {
            values.add(start + step * i);
        }
        return values;
    }
}
