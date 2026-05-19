package com.example.pptrefresh.tools;

import com.example.pptrefresh.funds.HardcodedFundCodeLookup;
import com.example.pptrefresh.sample.ZhongOuSampleData;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

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

    public DemoDataTools(HardcodedFundCodeLookup fundCodeLookup) {
        this.fundCodeLookup = fundCodeLookup;
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
                    "蓝海稳健增长混合A（示例）\n"
                            + "基金类型：偏股混合型基金\n"
                            + "成立日期：2019-06-12　　最新规模：58.6 亿元（示例）\n"
                            + "基金经理：张明、李悦（示例）\n"
                            + "业绩比较基准：沪深300指数收益率×70% + 中债综合指数×30%\n"
                            + "风险等级：R3（中风险）　　托管人：示例商业银行");
            m.put("note", "demo mock");
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

    @Tool("查询收益率排名表（yield_ranking_table task）。返回 cells 二维数组。")
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
            m.put("cells", ZhongOuSampleData.YIELD_RANKING_CELLS);
            m.put("note", "demo mock; 未按 tableRows/tableCols 裁剪");
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool(
            "查询大类资产配置分组柱状图数据（allocation_chart）。"
                    + "返回 categories、seriesNames、seriesValues。")
    public String fetchAllocationChart(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("报告截止季度，与用户消息 latestQuarter 一致") String latestQuarter,
            @P("横轴分类个数（样例 4）") int categoryCount,
            @P("系列个数（样例 4）") int seriesCount) {
        try {
            Map<String, Object> root = new LinkedHashMap<>(mockAllocationChartPayload());
            root.put("chartId", "allocation_chart");
            root.put("productCode", productCode);
            root.put("productName", productName);
            root.put("latestQuarter", latestQuarter);
            root.put("requestedCategoryCount", categoryCount);
            root.put("requestedSeriesCount", seriesCount);
            root.put("note", "demo mock");
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Tool("查询累计收益率折线图数据（nav_chart）。返回 categories、seriesNames、seriesValues。")
    public String fetchNavChart(
            @P("基金代码，与用户消息 fundCode 一致") String productCode,
            @P("产品展示名，与用户消息 productDisplayName 一致") String productName,
            @P("数据截止日，与用户消息 latestDate 一致") String latestDate,
            @P("业绩基准名称") String benchmarkName,
            @P("横轴分类个数（样例 6）") int categoryCount,
            @P("系列条数（样例 2）") int seriesCount) {
        try {
            Map<String, Object> root = new LinkedHashMap<>(mockNavChartPayload(productName, benchmarkName));
            root.put("chartId", "nav_chart");
            root.put("productCode", productCode);
            root.put("productName", productName);
            root.put("latestDate", latestDate);
            root.put("benchmarkName", benchmarkName);
            root.put("requestedCategoryCount", categoryCount);
            root.put("requestedSeriesCount", seriesCount);
            root.put("note", "demo mock");
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> mockAllocationChartPayload() {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("categories", ZhongOuSampleData.ALLOCATION_CATEGORIES);
        chart.put("seriesNames", ZhongOuSampleData.ALLOCATION_SERIES_NAMES);
        chart.put("seriesValues", ZhongOuSampleData.ALLOCATION_SERIES_VALUES);
        return chart;
    }

    private static Map<String, Object> mockNavChartPayload(String productName, String benchmarkName) {
        String fundSeries =
                (productName == null || productName.isBlank()) ? "本基金" : "本基金";
        String bench =
                (benchmarkName == null || benchmarkName.isBlank()) ? "业绩基准" : benchmarkName.trim();
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put(
                "categories",
                List.of(
                        "2024-05",
                        "2024-07",
                        "2024-09",
                        "2024-11",
                        "2025-01",
                        "2025-03",
                        "2025-05"));
        chart.put("seriesNames", List.of(fundSeries, bench));
        chart.put(
                "seriesValues",
                List.of(
                        List.of(0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0),
                        List.of(0.0, 1.5, 3.0, 4.5, 6.0, 7.5, 9.0)));
        return chart;
    }
}
