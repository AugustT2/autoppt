package com.example.pptrefresh.query;

import com.example.pptrefresh.document.ChartCategoryReader;
import com.example.pptrefresh.document.ResolvedTarget;
import com.example.pptrefresh.document.SlideStructure;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
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
    private final IntervalLexicon defaultLexicon =
            IntervalLexicon.load("/rules/lexicon/fund_performance_rows.yaml");

    public QueryPlanService(IntervalConditionResolver conditionResolver) {
        this.conditionResolver = conditionResolver;
    }

    public QueryPlan build(
            TaskDefinition task, ReportingContext reporting, ResolvedTarget target) {
        if (task.getType() == TaskType.text) {
            return null;
        }
        DimensionPolicy policy = resolvePolicy(task, target.structure());
        try {
            return switch (policy.getPolicyType()) {
                case "table_interval_labels", "table_interval_column" ->
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
        TableLabelAxis axis = resolveLabelAxis(policy);
        int labelIndex = resolveLabelIndex(policy);
        int headerSpan = resolveHeaderSpan(policy);
        TableIntervalDimensionExtractor.TableLabelScanResult scan =
                TableIntervalDimensionExtractor.scan(table, axis, labelIndex, headerSpan, lexicon);
        List<String> labels = scan.labels();
        if (labels.isEmpty()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "INTERVAL_LABELS_EMPTY",
                    "未从表格读到区间标签（axis="
                            + scan.axis()
                            + " index="
                            + scan.labelIndex()
                            + " headerSpan="
                            + scan.headerSpan()
                            + "）",
                    task.getId(),
                    null);
        }
        DimensionSlotRole dataRole =
                scan.axis() == TableLabelAxis.COLUMN
                        ? DimensionSlotRole.DATA_COLUMN
                        : DimensionSlotRole.DATA_ROW;
        List<DimensionSlot> slots = new ArrayList<>();
        List<String> headerLabels = scan.headerLabels();
        String headerCorner =
                headerLabels.isEmpty() ? "表头" : headerLabels.get(0);
        slots.add(
                new DimensionSlot(
                        0,
                        DimensionSlotRole.HEADER,
                        headerCorner,
                        null,
                        headerLabels,
                        scan.rowHeaderLabels()));
        int i = 0;
        for (String label : labels) {
            String kind = lexicon.resolveKind(label);
            if (kind == null) {
                throw new RefreshException(
                        FailureStage.CONDITION_RESOLVE,
                        "LEXICON_UNKNOWN_LABEL",
                        "词表无法识别区间标签: " + label,
                        task.getId(),
                        null);
            }
            QueryCondition condition = conditionResolver.resolve(kind, label, reporting);
            slots.add(new DimensionSlot(i + 1, dataRole, label, condition));
            i++;
        }
        QueryPlanWriteBack writeBack =
                new QueryPlanWriteBack(
                        table.getNumberOfRows(), table.getNumberOfColumns(), List.of());
        return new QueryPlan(
                task.getId(),
                reporting.asOfDate(),
                reporting.asOfQuarter(),
                slots,
                writeBack);
    }

    private QueryPlan buildQuarterChartPlan(
            TaskDefinition task, ReportingContext reporting, DimensionPolicy policy) {
        int categoryCount = policy.getCategoryCount() == null ? 4 : policy.getCategoryCount();
        List<String> quarters =
                DateQuarterUtil.rollingQuartersEndingAt(reporting.asOfQuarter(), categoryCount);
        List<DimensionSlot> slots = new ArrayList<>();
        int i = 0;
        for (String q : quarters) {
            slots.add(
                    new DimensionSlot(
                            i++, DimensionSlotRole.CATEGORY, q, QueryCondition.quarterPoint(q)));
        }
        QueryPlanWriteBack writeBack =
                new QueryPlanWriteBack(0, 0, quarters);
        return new QueryPlan(
                task.getId(),
                reporting.asOfDate(),
                reporting.asOfQuarter(),
                slots,
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
        List<DimensionSlot> slots = new ArrayList<>();
        int i = 0;
        for (String m : months) {
            slots.add(
                    new DimensionSlot(
                            i++, DimensionSlotRole.CATEGORY, m, QueryCondition.monthPoint(m)));
        }
        QueryPlanWriteBack writeBack = new QueryPlanWriteBack(0, 0, months);
        return new QueryPlan(
                task.getId(),
                reporting.asOfDate(),
                reporting.asOfQuarter(),
                slots,
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

    private static TableLabelAxis resolveLabelAxis(DimensionPolicy policy) {
        if (policy.getLabelAxis() != null && !policy.getLabelAxis().isBlank()) {
            return TableLabelAxis.fromYaml(policy.getLabelAxis());
        }
        if (policy.getIntervalColumn() != null) {
            return TableLabelAxis.ROW;
        }
        return TableLabelAxis.AUTO;
    }

    private static int resolveLabelIndex(DimensionPolicy policy) {
        if (policy.getLabelIndex() != null) {
            return policy.getLabelIndex();
        }
        if (policy.getIntervalColumn() != null) {
            return policy.getIntervalColumn();
        }
        return 0;
    }

    private static int resolveHeaderSpan(DimensionPolicy policy) {
        if (policy.getHeaderSpan() != null) {
            return policy.getHeaderSpan();
        }
        if (policy.getHeaderRows() != null) {
            return policy.getHeaderRows();
        }
        return 1;
    }

    private static DimensionPolicy defaultTablePolicy() {
        DimensionPolicy p = new DimensionPolicy();
        p.setPolicyType("table_interval_labels");
        p.setLabelAxis("AUTO");
        p.setLabelIndex(0);
        p.setHeaderSpan(1);
        p.setLexicon("/rules/lexicon/fund_performance_rows.yaml");
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
