package com.example.pptrefresh.tools;

import com.example.pptrefresh.funds.HardcodedFundCodeLookup;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示用数据 Tool：整页与 {@link com.example.pptrefresh.sample.ZhongOuDeckBuilder} / {@code 偏债混-M1.yaml} 对齐。
 * {@link Tool} 供 LangChain4j {@code langchain4j-spring-boot-starter} 扫描注册。
 */
@Component
public class DemoDataTools {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HardcodedFundCodeLookup fundCodeLookup;

    public DemoDataTools(HardcodedFundCodeLookup fundCodeLookup) {
        this.fundCodeLookup = fundCodeLookup;
    }

    /** 与模板第一列表头/示例单元格一致（7×6）。 */
    private static final List<List<String>> PERFORMANCE_CELLS =
            List.of(
                    List.of("指标", "2025年", "YTD", "任职以来", "近一年", "近六个月"),
                    List.of("累计收益", "7.87%", "4.63%", "21.08%", "13.39%", "6.20%"),
                    List.of("年化收益", "7.87%", "14.77%", "12%", "13.39%", "12.18%"),
                    List.of("收益排名", "--", "22%", "22%", "22%", "22%"),
                    List.of("二级债基指数(年化)", "5.76%", "7.08%", "7.91%", "7.58%", "4.76%"),
                    List.of("夏普比率", "1.36", "1.84", "1.65", "2.02", "1.60"),
                    List.of("最大回撤", "-2.81%", "-4.43%", "-4.43%", "-4.43%", "-4.43%"));

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

    @Tool(
            "联调用：一次返回整页硬编码演示数据（标题/文案后缀/业绩表/两张图）。"
                    + "productName 必须与用户消息中的 productDisplayName 一致；"
                    + "latestQuarter、latestDate 同用户消息。")
    public String fetchDeckDataBundle(String productName, String latestQuarter, String latestDate) {
        try {
            String display =
                    (productName == null || productName.isBlank()) ? "中欧瑾添" : productName.trim();

            Map<String, Object> allocation = new LinkedHashMap<>();
            allocation.put("categories", List.of("股票", "可转债", "利率债", "信用债"));
            allocation.put("seriesNames", List.of("2025Q2", "2025Q3", "2025Q4", "2026Q1"));
            allocation.put(
                    "seriesValues",
                    List.of(
                            List.of(29.0, 7.0, 23.0, 24.0),
                            List.of(35.0, 2.0, 20.0, 15.0),
                            List.of(38.0, 1.0, 20.0, 22.0),
                            List.of(32.0, 0.0, 12.0, 48.0)));

            Map<String, Object> nav = new LinkedHashMap<>();
            nav.put(
                    "categories",
                    List.of("2024-01", "2024-07", "2025-01", "2025-07", "2026-01", "2026-04"));
            nav.put("seriesNames", List.of("中欧瑾添A", "万得混合债券型二级指数"));
            nav.put(
                    "seriesValues",
                    List.of(
                            List.of(0.0, -2.0, 2.0, 8.0, 10.0, 12.5),
                            List.of(2.0, 4.0, 6.0, 10.0, 14.0, 16.0)));

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("deckType", "偏债混-M1");
            root.put("latestQuarter", latestQuarter);
            root.put("latestDate", latestDate);
            root.put("titleText", "偏债混-" + display);
            root.put(
                    "fundMetaAfterAnchor",
                    "  C:013999  |  成立日：2021-11-09（任职：2024-08-23）\n"
                            + "基金经理：王申、赵煜澄  |  最新规模：2.83亿");
            root.put(
                    "strategyAfterAnchor",
                    "\n以30%权益中枢的高波收益+策略，在控制波动与回撤的前提下增强收益。\n"
                            + "股票：采用smart beta宏观择时与自下而上宏观敏感度体系。\n"
                            + "可转债：采用估值交易指数体系与量化模型获取可持续alpha。\n"
                            + "纯债：高等级信用债为底仓，辅以利率交易。");
            root.put("performanceCells", PERFORMANCE_CELLS);
            root.put("allocationChart", allocation);
            root.put("navChart", nav);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
