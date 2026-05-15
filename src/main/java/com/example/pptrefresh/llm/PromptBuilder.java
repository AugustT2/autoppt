package com.example.pptrefresh.llm;

import com.example.pptrefresh.document.SlideStructure;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import com.example.pptrefresh.time.TimeContext;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public static final String SYSTEM =
            "你是 PPT 数据刷新助手。根据任务意图调用可用工具获取数据，最终只输出一段 JSON（不要 markdown 包裹），"
                    + "字段必须匹配任务类型："
                    + " text: {\"type\":\"text\",\"text\":\"...\"};"
                    + " table: {\"type\":\"table\",\"cells\":[[...],...]} 行列数必须与给定 dimensions 一致;"
                    + " chart: {\"type\":\"chart\",\"categories\":[...],\"seriesNames\":[...],\"seriesValues\":[[...],...]}。"
                    + "联调时可先调用 fetchDeckDataBundle(productName,latestQuarter,latestDate)，"
                    + "其中 productName 必须与下文 productDisplayName 一致，"
                    + "再按 taskId 选用 titleText、fundMetaAfterAnchor、strategyAfterAnchor、performanceCells、"
                    + "allocationChart、navChart 填入对应 JSON。"
                    + "禁止编造未在工具结果中出现的敏感字段；无法完成时仍输出合法 JSON 并在 text 中说明原因。";

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
        if (task.getHints() != null) {
            sb.append("hints=").append(task.getHints()).append('\n');
        }
        if (task.getType() == TaskType.table && structure != null) {
            sb.append("dimensions=rows:")
                    .append(structure.tableRows())
                    .append(",cols:")
                    .append(structure.tableCols())
                    .append('\n');
            if ("performance_table".equals(task.getId())) {
                sb.append("tableFieldGuide=cells 使用 fetchDeckDataBundle 返回 JSON 中的 performanceCells\n");
            }
        }
        if (task.getType() == TaskType.text) {
            sb.append("textFieldGuide=");
            switch (task.getId()) {
                case "title":
                    sb.append("用 bundle.titleText 作为整段替换（replace_all）");
                    break;
                case "fund_meta":
                    sb.append("用 bundle.fundMetaAfterAnchor：接在锚点 A:013998 之后，勿重复锚点");
                    break;
                case "strategy":
                    sb.append("用 bundle.strategyAfterAnchor：接在锚点 投资范围及策略 之后");
                    break;
                default:
                    sb.append("从 bundle 或工具结果选取与 intent 相符的纯文本");
                    break;
            }
            sb.append('\n');
        }
        if (task.getType() == TaskType.chart) {
            sb.append("chartFieldGuide=");
            if ("allocation_chart".equals(task.getId())) {
                sb.append("用 bundle.allocationChart 的 categories、seriesNames、seriesValues");
            } else if ("nav_chart".equals(task.getId())) {
                sb.append("用 bundle.navChart 的 categories、seriesNames、seriesValues");
            } else {
                sb.append("按 intent 从 bundle 选取对应 chart 字段");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
