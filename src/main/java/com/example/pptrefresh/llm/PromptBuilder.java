package com.example.pptrefresh.llm;

import com.example.pptrefresh.document.SlideStructure;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import com.example.pptrefresh.rules.TextReplaceMode;
import com.example.pptrefresh.query.QueryPlan;
import com.example.pptrefresh.query.QueryPlanFormatter;
import com.example.pptrefresh.query.ReportingContext;
import com.example.pptrefresh.time.TimeContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptBuilder {

    private final QueryPlanFormatter queryPlanFormatter;

    public PromptBuilder(QueryPlanFormatter queryPlanFormatter) {
        this.queryPlanFormatter = queryPlanFormatter;
    }

    /** Agent 模式：模型根据 taskType / taskId / intent 自选 Tool。 */
    public static final String SYSTEM_AGENT =
            """
            你是 PPT 数据刷新助手。

            ### 执行流程（必须遵守）
            1. 用 **function calling** 调用且仅调用 **一个** 合适的数据工具。
            2. 收到工具返回后，再 **单独** 输出一段写回 JSON（纯 JSON，不要 markdown 代码块）。

            ### 禁止事项
            - 禁止跳过步骤 1 直接输出写回 JSON。
            - 禁止把 **工具返回体** 当作最终答案（例如只输出 `scaleValue`、`fundMetaAfterAnchor`、`cells`、`categories` 等）。
            - 禁止照抄用户消息里 **写回结构示例** 中的占位符或示例数字。
            - 禁止把整段写回 JSON 再塞进 `text` 字符串（chart/table 字段须在 JSON 顶层）。

            ### 工具选型
            | 场景 | 工具 |
            |------|------|
            | taskId=title | fetchTitleText |
            | taskId=fund_meta | fetchFundMetaAfterAnchor |
            | mode=replace_labeled_number 或 taskId=fund_latest_scale | fetchFundLatestScale |
            | taskType=table | fetchPerformanceTable |
            | taskId=allocation_chart | fetchAllocationChart |
            | 净值 / 累计收益折线 | fetchNavChart |

            ### 数据规则
            - 表格：按用户消息中的 `queryPlan` 查数，勿自行推算区间日期。
            - 写回字段中的数值与文案必须来自工具返回，不得编造。
            """;

    /** 流水线模式：YAML 已指定 tool，必须只调该工具。 */
    public static final String SYSTEM_STRICT =
            """
            你是 PPT 数据刷新助手。

            ### 执行流程（必须遵守）
            1. 必须调用用户消息 **「指定工具」** 一节中的唯一工具。
            2. 收到工具返回后，再 **单独** 输出写回 JSON（纯 JSON，不要 markdown 代码块）。

            ### 禁止事项
            - 禁止跳过工具直接写回。
            - 禁止输出工具返回体（如 `scaleValue`）作为最终答案。
            - 禁止照抄 **写回结构示例** 中的占位符或示例数字。
            """;

    public String systemMessage(TaskDefinition task) {
        return isStrictMode(task) ? SYSTEM_STRICT : SYSTEM_AGENT;
    }

    public String buildUserMessage(
            String deckType,
            String productDisplayName,
            String fundCode,
            TimeContext time,
            ReportingContext reporting,
            QueryPlan queryPlan,
            TaskDefinition task,
            SlideStructure structure) {
        StringBuilder sb = new StringBuilder();
        appendReportingContext(sb, deckType, productDisplayName, fundCode, time, reporting);
        appendCurrentTask(sb, task, deckType, structure);
        if (isStrictMode(task)) {
            appendStrictTool(sb, task);
        } else {
            appendToolMapping(sb, task);
        }
        appendWritebackShape(sb, task);
        if (queryPlan != null) {
            appendQueryPlan(sb, queryPlan);
        }
        if (task.getHints() != null && !task.getHints().isBlank()) {
            sb.append("\n### 补充提示\n").append(task.getHints().trim()).append('\n');
        }
        return sb.toString().trim();
    }

    private static void appendReportingContext(
            StringBuilder sb,
            String deckType,
            String productDisplayName,
            String fundCode,
            TimeContext time,
            ReportingContext reporting) {
        sb.append(
                """
                ### 报告上下文

                | 字段 | 值 |
                |------|-----|
                """);
        row(sb, "deckType", deckType);
        row(sb, "productDisplayName", productDisplayName);
        row(sb, "fundCode", fundCode);
        row(sb, "latestDate", time.latestDate().toString());
        row(sb, "latestQuarter", time.latestQuarter());
        if (reporting != null) {
            row(sb, "fundInceptionDate", reporting.fundInceptionDate().toString());
            row(sb, "managerTenureStartDate", reporting.managerTenureStartDate().toString());
        }
    }

    private static void appendCurrentTask(
            StringBuilder sb, TaskDefinition task, String deckType, SlideStructure structure) {
        sb.append(
                """
                \n### 当前任务

                | 字段 | 值 |
                |------|-----|
                """);
        row(sb, "taskId", task.getId());
        row(sb, "taskType", task.getType().name());
        if (task.getMode() != null) {
            row(sb, "mode", task.getMode().name());
        }
        if (StringUtils.hasText(task.getFieldLabel())) {
            row(sb, "fieldLabel", task.getFieldLabel().trim());
        }
        String prefix = productLinePrefixFromDeckType(deckType);
        if (prefix != null) {
            row(sb, "productLinePrefix", prefix);
        }
        if (task.getType() == TaskType.table && structure != null) {
            row(sb, "dimensions.rows", String.valueOf(structure.tableRows()));
            row(sb, "dimensions.cols", String.valueOf(structure.tableCols()));
        }
        sb.append("\n**任务说明（intent）**\n\n").append(task.getIntent()).append('\n');
    }

    private static void appendStrictTool(StringBuilder sb, TaskDefinition task) {
        sb.append(
                """

                ### 指定工具

                本任务仅允许：`%s`
                """
                        .formatted(task.getTool().trim()));
    }

    private static void appendToolMapping(StringBuilder sb, TaskDefinition task) {
        String mapping = toolWritebackMappingMarkdown(task);
        if (mapping == null) {
            return;
        }
        sb.append("\n### 工具与写回映射\n\n").append(mapping);
    }

    private static void appendWritebackShape(StringBuilder sb, TaskDefinition task) {
        sb.append(
                """

                ### 写回 JSON 结构（示例，非真实数据）

                以下仅为 **字段形状**；`text` / `cells` 等取值必须来自工具返回，**禁止照抄占位符**。

                ```json
                %s
                ```
                """
                        .formatted(writebackJsonExample(task)));
    }

    private void appendQueryPlan(StringBuilder sb, QueryPlan queryPlan) {
        sb.append("\n### QueryPlan（已预计算，勿改日期）\n\n```json\n");
        sb.append(queryPlanFormatter.toJson(queryPlan));
        sb.append("\n```\n");
    }

    private static void row(StringBuilder sb, String key, String value) {
        sb.append("| ").append(key).append(" | ").append(value).append(" |\n");
    }

    private static boolean isStrictMode(TaskDefinition task) {
        return StringUtils.hasText(task.getTool());
    }

    private static String productLinePrefixFromDeckType(String deckType) {
        if (deckType == null || !deckType.contains("-")) {
            return null;
        }
        return deckType.substring(0, deckType.indexOf('-'));
    }

    private static String toolWritebackMappingMarkdown(TaskDefinition task) {
        if (task.getType() != TaskType.text) {
            return null;
        }
        if (task.getMode() == TextReplaceMode.replace_labeled_number) {
            return """
                    - **应调用工具**：`fetchFundLatestScale`
                    - **参数**：`productCode`=fundCode，`fieldLabel`=fieldLabel
                    - **映射**：工具返回 `scaleValue` → 写回字段 `text`（仅数字，无单位）
                    """;
        }
        if ("fund_meta".equals(task.getId())) {
            return """
                    - **应调用工具**：`fetchFundMetaAfterAnchor`
                    - **映射**：`fundMetaAfterAnchor` → 写回字段 `text`
                    """;
        }
        if ("title".equals(task.getId())) {
            return """
                    - **应调用工具**：`fetchTitleText`
                    - **映射**：`titleText` → 写回字段 `text`
                    """;
        }
        return null;
    }

    private static String writebackJsonExample(TaskDefinition task) {
        if (task.getType() == TaskType.text
                && task.getMode() == TextReplaceMode.replace_labeled_number) {
            return """
                    {
                      "type": "text",
                      "text": "<来自 fetchFundLatestScale.scaleValue 的数字>"
                    }""";
        }
        return switch (task.getType()) {
            case text ->
                    """
                    {
                      "type": "text",
                      "text": "<来自对应工具返回字段的文案>"
                    }""";
            case table ->
                    """
                    {
                      "type": "table",
                      "cells": [["...", "..."]]
                    }""";
            case chart ->
                    """
                    {
                      "type": "chart",
                      "categories": ["..."],
                      "seriesNames": ["..."],
                      "seriesValues": [[0.0]]
                    }""";
            default -> throw new IllegalArgumentException("未知 task type: " + task.getType());
        };
    }
}
