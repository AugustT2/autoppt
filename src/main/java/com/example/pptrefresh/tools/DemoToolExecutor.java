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
                        "fetchDeckDataBundle",
                        "联调用：一次返回整页硬编码演示数据（标题/文案后缀/业绩表/两张图）。"
                                + "productName 必须与用户消息中的 productDisplayName 一致；"
                                + "latestQuarter、latestDate 同用户消息。",
                        "productName",
                        "latestQuarter",
                        "latestDate"));
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
            case "fetchDeckDataBundle":
                return tools.fetchDeckDataBundle(
                        args.path("productName").asText(""),
                        args.path("latestQuarter").asText(""),
                        args.path("latestDate").asText(""));
            default:
                throw new IllegalArgumentException("未知 tool: " + name);
        }
    }
}
