package com.example.pptrefresh.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DemoToolExecutor {

    private final DemoDataTools tools;
    private final ObjectMapper mapper = new ObjectMapper();

    public DemoToolExecutor(DemoDataTools tools) {
        this.tools = tools;
    }

    public List<Map<String, Object>> toolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(wrapFunction("lookupProductCode", "根据产品名称查询产品代码", "productName"));
        tools.add(
                wrapFunction(
                        "fetchQuarterReturnSummary",
                        "查询产品在指定季度的收益率摘要",
                        "productCode",
                        "quarter"));
        tools.add(
                wrapFunction(
                        "fetchTitleText",
                        "查询主标题文案（title task）",
                        "productName",
                        "productLinePrefix"));
        tools.add(
                wrapFunction(
                        "fetchFundMetaAfterAnchor",
                        "查询基金基本信息后缀（fund_meta task）",
                        "productCode",
                        "productName",
                        "latestQuarter"));
        tools.add(
                wrapFunction(
                        "fetchStrategyAfterAnchor",
                        "查询投资范围及策略正文（strategy task）",
                        "productCode",
                        "productName"));
        tools.add(
                wrapFunction(
                        "fetchPerformanceTable",
                        "查询业绩指标表（performance_table task）",
                        "productCode",
                        "productName",
                        "latestQuarter",
                        "tableRows",
                        "tableCols"));
        tools.add(
                wrapFunction(
                        "fetchAllocationChart",
                        "查询大类资产配置柱状图（allocation_chart）",
                        "productCode",
                        "productName",
                        "latestQuarter",
                        "categoryCount",
                        "seriesCount"));
        tools.add(
                wrapFunction(
                        "fetchNavChart",
                        "查询累计收益率折线图（nav_chart）",
                        "productCode",
                        "productName",
                        "latestDate",
                        "benchmarkName",
                        "categoryCount",
                        "seriesCount"));
        return tools;
    }

    private static Map<String, Object> wrapFunction(String name, String description, String... required) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String field : required) {
            properties.put(field, Map.of("type", "string"));
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of(required));
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    public String execute(String name, String argumentsJson) throws Exception {
        JsonNode args = mapper.readTree(argumentsJson);
        switch (name) {
            case "lookupProductCode":
                return tools.lookupProductCode(args.get("productName").asText());
            case "fetchQuarterReturnSummary":
                return tools.fetchQuarterReturnSummary(
                        args.get("productCode").asText(), args.get("quarter").asText());
            case "fetchTitleText":
                return tools.fetchTitleText(
                        args.path("productName").asText(""),
                        args.path("productLinePrefix").asText("偏债混"));
            case "fetchFundMetaAfterAnchor":
                return tools.fetchFundMetaAfterAnchor(
                        args.path("productCode").asText(""),
                        args.path("productName").asText(""),
                        args.path("latestQuarter").asText(""));
            case "fetchStrategyAfterAnchor":
                return tools.fetchStrategyAfterAnchor(
                        args.path("productCode").asText(""),
                        args.path("productName").asText(""));
            case "fetchPerformanceTable":
                return tools.fetchPerformanceTable(
                        args.path("productCode").asText(""),
                        args.path("productName").asText(""),
                        args.path("latestQuarter").asText(""),
                        args.path("tableRows").asInt(7),
                        args.path("tableCols").asInt(6));
            case "fetchAllocationChart":
                return tools.fetchAllocationChart(
                        args.path("productCode").asText(""),
                        args.path("productName").asText(""),
                        args.path("latestQuarter").asText(""),
                        args.path("categoryCount").asInt(4),
                        args.path("seriesCount").asInt(4));
            case "fetchNavChart":
                return tools.fetchNavChart(
                        args.path("productCode").asText(""),
                        args.path("productName").asText(""),
                        args.path("latestDate").asText(""),
                        args.path("benchmarkName").asText("万得混合债券型二级指数"),
                        args.path("categoryCount").asInt(6),
                        args.path("seriesCount").asInt(2));
            default:
                throw new IllegalArgumentException("未知 tool: " + name);
        }
    }
}
