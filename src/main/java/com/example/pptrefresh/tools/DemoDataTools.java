package com.example.pptrefresh.tools;

import com.example.pptrefresh.funds.FundFactsClient;
import com.example.pptrefresh.funds.HardcodedFundCodeLookup;
import com.example.pptrefresh.query.ChartSeriesData;
import com.example.pptrefresh.query.QueryPlan;
import com.example.pptrefresh.query.QueryPlanDataService;
import com.example.pptrefresh.query.QueryPlanRequired;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** 演示用数据 Tool；表格/图表必须带 {@link QueryPlan}。 */
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
            "查询收益率排名矩阵表（yield_ranking_table）。返回 cells；"
                    + "按 queryPlan 的 intervals×metrics 查数（横排区间、纵排指标）。")
    public String fetchPerformanceTable(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("报告截止季度，与用户消息 latestQuarter 一致") String latestQuarter,
            @P("表格行数，与用户消息 dimensions.rows 一致") int tableRows,
            @P("表格列数，与用户消息 dimensions.cols 一致") int tableCols) {
        try {
            QueryPlan plan = QueryPlanRequired.fromTaskContext();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productCode", productCode);
            m.put("productName", productName);
            m.put("latestQuarter", latestQuarter);
            m.put("requestedTableRows", tableRows);
            m.put("requestedTableCols", tableCols);
            m.put("cells", queryPlanDataService.buildTableCells(plan, productCode, tableRows, tableCols));
            m.put("queryPlanUsed", true);
            m.put("note", "按 queryPlan 逐区间查表");
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool(
            "查询大类资产配置分组柱状图（allocation_chart）：categories=股票/可转债/利率债/信用债，series=季度。"
                    + "返回 categories、seriesNames、seriesValues；按 queryPlan 各季度查数。")
    public String fetchAllocationChart(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("报告截止季度，与用户消息 latestQuarter 一致") String latestQuarter,
            @P("横轴分类个数") int categoryCount,
            @P("系列个数") int seriesCount,
            @P("可选：queryPlan.writeBack.categoryLabels 的 JSON 数组字符串") String categoryLabelsJson) {
        try {
            QueryPlan plan = QueryPlanRequired.fromTaskContext();
            ChartSeriesData data = queryPlanDataService.buildAllocationChart(plan, productCode);
            Map<String, Object> root = chartMap(data);
            root.put("chartId", "allocation_chart");
            root.put("productCode", productCode);
            root.put("productName", productName);
            root.put("latestQuarter", latestQuarter);
            root.put("requestedCategoryCount", categoryCount);
            root.put("requestedSeriesCount", seriesCount);
            root.put("queryPlanUsed", true);
            root.put("note", "按 queryPlan 逐季度查资产配置");
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool(
            "查询累计收益率折线图数据（nav_chart）。返回 categories、seriesNames、seriesValues；"
                    + "按 queryPlan.navTimeRange 起止日批量取数，再对齐横轴标签。")
    public String fetchNavChart(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("数据截止日，与用户消息 latestDate 一致") String latestDate,
            @P("业绩基准名称") String benchmarkName,
            @P("横轴分类个数") int categoryCount,
            @P("系列条数") int seriesCount,
            @P("可选：queryPlan.writeBack.categoryLabels 的 JSON 数组字符串") String categoryLabelsJson) {
        try {
            QueryPlan plan = QueryPlanRequired.fromTaskContext();
            ChartSeriesData data = queryPlanDataService.buildNavChart(plan, productCode);
            Map<String, Object> root = chartMap(data);
            root.put("chartId", "nav_chart");
            root.put("productCode", productCode);
            root.put("productName", productName);
            root.put("latestDate", latestDate);
            root.put("benchmarkName", benchmarkName);
            root.put("requestedCategoryCount", categoryCount);
            root.put("requestedSeriesCount", seriesCount);
            root.put("queryPlanUsed", true);
            root.put("note", "按 queryPlan 逐月份查累计收益");
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> chartMap(ChartSeriesData data) {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("categories", data.categories());
        chart.put("seriesNames", data.seriesNames());
        chart.put("seriesValues", data.seriesValues());
        return chart;
    }
}
