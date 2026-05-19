package com.example.pptrefresh.llm;

import com.example.pptrefresh.document.SlideStructure;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import com.example.pptrefresh.time.TimeContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptBuilder {

    /** Agent 模式：模型根据 taskType / taskId / intent 自选 Tool。 */
    public static final String SYSTEM_AGENT =
            "你是 PPT 数据刷新助手。根据用户消息中的 taskType、taskId、intent，从可用工具中选择合适的一个获取数据，"
                    + "再只输出一段写回 JSON（不要 markdown）。参数名与含义见各工具 schema，"
                    + "值可从 fundCode、productDisplayName、latestDate、latestQuarter、productLinePrefix、dimensions 等上下文填写。"
                    + "选型参考：taskType=text 且 taskId=title → fetchTitleText；"
                    + "taskId=fund_meta → fetchFundMetaAfterAnchor；"
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
            TaskDefinition task,
            SlideStructure structure) {
        StringBuilder sb = new StringBuilder();
        sb.append("deckType=").append(deckType).append('\n');
        sb.append("productDisplayName=").append(productDisplayName).append('\n');
        sb.append("fundCode=").append(fundCode).append('\n');
        sb.append("latestDate=").append(time.latestDate()).append('\n');
        sb.append("latestQuarter=").append(time.latestQuarter()).append('\n');
        sb.append("taskId=").append(task.getId()).append('\n');
        sb.append("taskType=").append(task.getType()).append('\n');
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
        sb.append("writeback=").append(writebackShape(task.getType())).append('\n');
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

    private static String writebackShape(TaskType type) {
        switch (type) {
            case text:
                return "{\"type\":\"text\",\"text\":\"...\"}";
            case table:
                return "{\"type\":\"table\",\"cells\":[[...],...]} 行列与 dimensions 一致";
            case chart:
                return "{\"type\":\"chart\",\"categories\":[...],\"seriesNames\":[...],\"seriesValues\":[[...],...]}";
            default:
                throw new IllegalArgumentException("未知 task type: " + type);
        }
    }
}
