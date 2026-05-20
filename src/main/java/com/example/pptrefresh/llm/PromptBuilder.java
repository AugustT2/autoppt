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
            "你是 PPT 数据刷新助手。根据用户消息中的 taskType、taskId、intent，从可用工具中选择合适的一个获取数据，"
                    + "再只输出一段写回 JSON（不要 markdown）。chart/table 的 categories、cells 等字段必须在 JSON 顶层，"
                    + "禁止把整个写回对象再塞进 text 字符串。参数名与含义见各工具 schema，"
                    + "值可从 fundCode、productDisplayName、latestDate、latestQuarter、queryPlan、productLinePrefix、dimensions 等上下文填写。"
                    + "表格 queryPlan 含 headers、metrics、intervals(含已解析的 condition)，勿自行推算日期；"
                    + "fetchPerformanceTable 按 queryPlan 对每个区间×指标查数；图表类按 intervals/categories 查数。"
                    + "选型参考：taskType=text 且 taskId=title → fetchTitleText；"
                    + "taskId=fund_meta → fetchFundMetaAfterAnchor；"
                    + "mode=replace_labeled_number 或 taskId=fund_latest_scale → fetchFundLatestScale，写回 text 仅数字；"
                    + "taskType=table → fetchPerformanceTable；"
                    + "taskType=chart 且 intent 含资产配置或柱状图 → fetchAllocationChart（categories=季度，series=资产类别）；"
                    + "含净值、累计收益或折线 → fetchNavChart。"
                    + "禁止编造工具结果中未出现的字段；无法完成时仍输出合法 JSON 并在 text 中说明原因。";

    /** 流水线模式：YAML 已指定 tool，必须只调该工具。 */
    public static final String SYSTEM_STRICT =
            "你是 PPT 数据刷新助手。用户消息中 tool= 指定了本任务唯一允许的工具；"
                    + "你必须先调用该工具（参数见 schema 与上下文），再只输出写回 JSON（不要 markdown）。"
                    + "禁止编造工具结果中未出现的字段；无法完成时仍输出合法 JSON 并在 text 中说明原因。";

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
        sb.append("deckType=").append(deckType).append('\n');
        sb.append("productDisplayName=").append(productDisplayName).append('\n');
        sb.append("fundCode=").append(fundCode).append('\n');
        sb.append("latestDate=").append(time.latestDate()).append('\n');
        sb.append("latestQuarter=").append(time.latestQuarter()).append('\n');
        if (reporting != null) {
            sb.append("fundInceptionDate=").append(reporting.fundInceptionDate()).append('\n');
            sb.append("managerTenureStartDate=")
                    .append(reporting.managerTenureStartDate())
                    .append('\n');
        }
        if (queryPlan != null) {
            sb.append("queryPlan=").append(queryPlanFormatter.toJson(queryPlan)).append('\n');
        }
        sb.append("taskId=").append(task.getId()).append('\n');
        sb.append("taskType=").append(task.getType()).append('\n');
        if (task.getMode() != null) {
            sb.append("mode=").append(task.getMode()).append('\n');
        }
        if (StringUtils.hasText(task.getFieldLabel())) {
            sb.append("fieldLabel=").append(task.getFieldLabel().trim()).append('\n');
        }
        sb.append("intent=").append(task.getIntent()).append('\n');

        if (isStrictMode(task)) {
            sb.append("tool=").append(task.getTool().trim()).append('\n');
        }

        String productLinePrefix = productLinePrefixFromDeckType(deckType);
        if (productLinePrefix != null) {
            sb.append("productLinePrefix=").append(productLinePrefix).append('\n');
        }
        if (task.getType() == TaskType.table && structure != null) {
            sb.append("dimensions=rows:")
                    .append(structure.tableRows())
                    .append(",cols:")
                    .append(structure.tableCols())
                    .append('\n');
        }
        sb.append("writeback=").append(writebackShape(task)).append('\n');
        if (task.getHints() != null && !task.getHints().isBlank()) {
            sb.append("hints=").append(task.getHints().trim()).append('\n');
        }
        return sb.toString();
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

    private static String writebackShape(TaskDefinition task) {
        if (task.getType() == TaskType.text
                && task.getMode() == TextReplaceMode.replace_labeled_number) {
            return "{\"type\":\"text\",\"text\":\"58.6\"} 仅数字，不含亿元等单位；来自 tool 的 scaleValue";
        }
        switch (task.getType()) {
            case text:
                return "{\"type\":\"text\",\"text\":\"...\"}";
            case table:
                return "{\"type\":\"table\",\"cells\":[[...],...]} 行列与 dimensions 一致";
            case chart:
                return "{\"type\":\"chart\",\"categories\":[...],\"seriesNames\":[...],\"seriesValues\":[[...],...]} "
                        + "勿用 {\"text\":\"{...}\"} 包裹";
            default:
                throw new IllegalArgumentException("未知 task type: " + task.getType());
        }
    }
}
