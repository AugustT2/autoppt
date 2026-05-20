package com.example.pptrefresh.query;

import com.example.pptrefresh.document.ChartCategoryReader;
import com.example.pptrefresh.document.ResolvedTarget;
import com.example.pptrefresh.document.SlideStructure;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.query.metric.MetricCatalog;
import com.example.pptrefresh.rules.DimensionPolicy;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryPlanService {

    private final IntervalConditionResolver conditionResolver;
    private final TableQueryInferenceService tableQueryInferenceService;
    private final IntervalLexicon defaultLexicon =
            IntervalLexicon.load("/rules/lexicon/fund_performance_rows.yaml");

    public QueryPlanService(
            IntervalConditionResolver conditionResolver,
            TableQueryInferenceService tableQueryInferenceService) {
        this.conditionResolver = conditionResolver;
        this.tableQueryInferenceService = tableQueryInferenceService;
    }

    public QueryPlan build(
            TaskDefinition task, ReportingContext reporting, ResolvedTarget target) {
        if (task.getType() == TaskType.text) {
            return null;
        }
        DimensionPolicy policy = resolvePolicy(task, target.structure());
        try {
            return switch (policy.getPolicyType()) {
                case "table_interval_labels" ->
                        buildTablePlan(task, reporting, target, policy);
                case "quarter_series" -> buildQuarterChartPlan(task, reporting, policy);
                case "month_series" -> buildMonthChartPlan(task, reporting, target, policy);
                default ->
                        throw new IllegalArgumentException(
                                "未知 dimensionPolicy.policyType: " + policy.getPolicyType());
            };
        } catch (RefreshException e) {
            throw e;
        } catch (Exception e) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "QUERY_PLAN_BUILD_FAILED",
                    "无法构建 QueryPlan: " + e.getMessage(),
                    task.getId(),
                    e);
        }
    }

    private QueryPlan buildTablePlan(
            TaskDefinition task,
            ReportingContext reporting,
            ResolvedTarget target,
            DimensionPolicy policy) {
        XSLFTable table = target.table();
        if (table == null) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "TABLE_MISSING",
                    "表格目标为空",
                    task.getId(),
                    null);
        }
        String lexiconPath =
                policy.getLexicon() == null
                        ? "/rules/lexicon/fund_performance_rows.yaml"
                        : policy.getLexicon();
        IntervalLexicon lexicon = loadLexicon(lexiconPath);
        TableAnalysis analysis = tableQueryInferenceService.analyze(task, table, lexicon);
        DimensionSlotRole dataRole =
                analysis.intervalAxis() == TableLabelAxis.COLUMN
                        ? DimensionSlotRole.DATA_COLUMN
                        : DimensionSlotRole.DATA_ROW;
        List<DimensionSlot> dimensions = new ArrayList<>();
        dimensions.add(
                new DimensionSlot(
                        0,
                        DimensionSlotRole.HEADER,
                        "",
                        null,
                        analysis.columnHeaders(),
                        analysis.rowHeaders()));
        int i = 0;
        for (String label : analysis.intervalLabels()) {
            String kind = lexicon.resolveKind(label);
            QueryCondition condition = conditionResolver.resolve(kind, label, reporting);
            dimensions.add(new DimensionSlot(i + 1, dataRole, label, condition));
            i++;
        }
        QueryPlanWriteBack writeBack =
                new QueryPlanWriteBack(
                        table.getNumberOfRows(), table.getNumberOfColumns(), List.of());
        String metricsLexicon =
                policy.getMetricsLexicon() == null
                        ? MetricCatalog.DEFAULT_RESOURCE
                        : policy.getMetricsLexicon();
        return new QueryPlan(
                task.getId(),
                reporting.asOfDate(),
                reporting.asOfQuarter(),
                dimensions,
                writeBack,
                analysis.metrics(),
                metricsLexicon);
    }

    private QueryPlan buildQuarterChartPlan(
            TaskDefinition task, ReportingContext reporting, DimensionPolicy policy) {
        int categoryCount = policy.getCategoryCount() == null ? 4 : policy.getCategoryCount();
        List<String> quarters =
                DateQuarterUtil.rollingQuartersEndingAt(reporting.asOfQuarter(), categoryCount);
        List<DimensionSlot> dimensions = new ArrayList<>();
        int i = 0;
        for (String q : quarters) {
            dimensions.add(
                    new DimensionSlot(
                            i++, DimensionSlotRole.CATEGORY, q, QueryCondition.quarterPoint(q)));
        }
        QueryPlanWriteBack writeBack =
                new QueryPlanWriteBack(0, 0, quarters);
        return new QueryPlan(
                task.getId(),
                reporting.asOfDate(),
                reporting.asOfQuarter(),
                dimensions,
                writeBack);
    }

    private QueryPlan buildMonthChartPlan(
            TaskDefinition task,
            ReportingContext reporting,
            ResolvedTarget target,
            DimensionPolicy policy) {
        Integer monthCount = policy.getMonthPointCount();
        int count;
        if (monthCount == null || monthCount <= 0) {
            List<String> existing = ChartCategoryReader.readCategories(target.chart());
            count = existing.isEmpty() ? 7 : existing.size();
        } else {
            count = monthCount;
        }
        List<String> months =
                DateQuarterUtil.rollingMonthsEndingAt(reporting.asOfDate(), count);
        List<DimensionSlot> dimensions = new ArrayList<>();
        int i = 0;
        for (String m : months) {
            dimensions.add(
                    new DimensionSlot(
                            i++, DimensionSlotRole.CATEGORY, m, QueryCondition.monthPoint(m)));
        }
        QueryPlanWriteBack writeBack = new QueryPlanWriteBack(0, 0, months);
        return new QueryPlan(
                task.getId(),
                reporting.asOfDate(),
                reporting.asOfQuarter(),
                dimensions,
                writeBack);
    }

    private IntervalLexicon loadLexicon(String path) {
        String resource = path.startsWith("/") ? path : "/rules/lexicon/" + path;
        if (!resource.endsWith(".yaml")) {
            resource = resource + ".yaml";
        }
        return IntervalLexicon.load(resource);
    }

    private DimensionPolicy resolvePolicy(TaskDefinition task, SlideStructure structure) {
        if (task.getDimensionPolicy() != null) {
            return task.getDimensionPolicy();
        }
        return switch (task.getType()) {
            case table -> defaultTablePolicy();
            case chart -> defaultChartPolicy(task, structure);
            default -> throw new IllegalArgumentException("text 任务无 QueryPlan");
        };
    }

    private static DimensionPolicy defaultTablePolicy() {
        DimensionPolicy p = new DimensionPolicy();
        p.setPolicyType("table_interval_labels");
        p.setLexicon("/rules/lexicon/fund_performance_rows.yaml");
        p.setMetricsLexicon(MetricCatalog.DEFAULT_RESOURCE);
        return p;
    }

    private static DimensionPolicy defaultChartPolicy(TaskDefinition task, SlideStructure structure) {
        DimensionPolicy p = new DimensionPolicy();
        if ("nav_chart".equals(task.getId())
                || (task.getIntent() != null && task.getIntent().contains("折线"))) {
            p.setPolicyType("month_series");
            if (structure != null && structure.categoryCount() > 0) {
                p.setMonthPointCount(structure.categoryCount());
            } else {
                p.setMonthPointCount(7);
            }
        } else {
            p.setPolicyType("quarter_series");
            p.setCategoryCount(4);
        }
        return p;
    }
}
