package com.example.pptrefresh.query;

import com.example.pptrefresh.document.ChartCategoryReader;
import com.example.pptrefresh.document.ChartSeriesReader;
import com.example.pptrefresh.document.ResolvedTarget;
import com.example.pptrefresh.document.SlideStructure;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.query.metric.MetricCatalog;
import com.example.pptrefresh.rules.DimensionPolicy;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryPlanService {

    private static final Logger log = LoggerFactory.getLogger(QueryPlanService.class);

    private final IntervalConditionResolver conditionResolver;
    private final TableQueryInferenceService tableQueryInferenceService;
    private final NavChartTimeRangeResolver navChartTimeRangeResolver;
    private final IntervalLexicon defaultLexicon =
            IntervalLexicon.load("/rules/lexicon/fund_performance_rows.yaml");

    public QueryPlanService(
            IntervalConditionResolver conditionResolver,
            TableQueryInferenceService tableQueryInferenceService,
            NavChartTimeRangeResolver navChartTimeRangeResolver) {
        this.conditionResolver = conditionResolver;
        this.tableQueryInferenceService = tableQueryInferenceService;
        this.navChartTimeRangeResolver = navChartTimeRangeResolver;
    }

    public QueryPlan build(
            TaskDefinition task, ReportingContext reporting, ResolvedTarget target) {
        return build(task, reporting, target, null);
    }

    public QueryPlan build(
            TaskDefinition task,
            ReportingContext reporting,
            ResolvedTarget target,
            String productDisplayName) {
        if (task.getType() == TaskType.text) {
            return null;
        }
        DimensionPolicy policy = resolvePolicy(task, target.structure());
        try {
            return switch (policy.getPolicyType()) {
                case "table_interval_labels" ->
                        buildTablePlan(task, reporting, target, policy);
                case "asset_class_allocation" ->
                        buildAssetClassAllocationPlan(task, reporting, policy);
                case "nav_line_series" ->
                        buildNavLineSeriesPlan(
                                task, reporting, target, policy, productDisplayName);
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
        log.info("构建表格 QueryPlan task={} lexicon={}", task.getId(), lexiconPath);
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

    private QueryPlan buildAssetClassAllocationPlan(
            TaskDefinition task, ReportingContext reporting, DimensionPolicy policy) {
        List<String> categories = policy.getChartCategories();
        if (categories == null || categories.isEmpty()) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "CHART_CATEGORIES_EMPTY",
                    "asset_class_allocation 需要 chartCategories",
                    task.getId(),
                    null);
        }
        int seriesCount = resolveAssetAllocationSeriesCount(policy);
        List<String> quarters =
                DateQuarterUtil.rollingQuartersEndingAt(reporting.asOfQuarter(), seriesCount);
        List<DimensionSlot> dimensions = new ArrayList<>();
        int i = 0;
        for (String q : quarters) {
            dimensions.add(
                    new DimensionSlot(
                            i++, DimensionSlotRole.CATEGORY, q, QueryCondition.quarterPoint(q)));
        }
        QueryPlanWriteBack writeBack = new QueryPlanWriteBack(0, 0, categories);
        return new QueryPlan(
                task.getId(),
                reporting.asOfDate(),
                reporting.asOfQuarter(),
                dimensions,
                writeBack,
                null,
                null,
                quarters);
    }

    private static int resolveAssetAllocationSeriesCount(DimensionPolicy policy) {
        if (policy.getChartSeriesNames() != null && !policy.getChartSeriesNames().isEmpty()) {
            return policy.getChartSeriesNames().size();
        }
        if (policy.getCategoryCount() != null && policy.getCategoryCount() > 0) {
            return policy.getCategoryCount();
        }
        return 4;
    }

    private QueryPlan buildNavLineSeriesPlan(
            TaskDefinition task,
            ReportingContext reporting,
            ResolvedTarget target,
            DimensionPolicy policy,
            String productDisplayName) {
        if (target.chart() == null) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "CHART_MISSING",
                    "折线图目标为空",
                    task.getId(),
                    null);
        }
        List<String> categories = ChartCategoryReader.readCategories(target.chart());
        if (categories.isEmpty()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "CHART_CATEGORIES_EMPTY",
                    "模板折线图无横轴分类，无法构建 nav_line_series",
                    task.getId(),
                    null);
        }
        List<String> seriesLabels = ChartSeriesReader.readSeriesNames(target.chart());
        if (seriesLabels.isEmpty()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "CHART_SERIES_EMPTY",
                    "模板折线图无系列名",
                    task.getId(),
                    null);
        }
        if (productDisplayName != null
                && !productDisplayName.isBlank()
                && !FundLabelMatcher.matches(seriesLabels.get(0), productDisplayName)) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "NAV_FUND_LABEL_MISMATCH",
                    "折线图首条系列「"
                            + seriesLabels.get(0)
                            + "」与产品名「"
                            + productDisplayName
                            + "」不一致",
                    task.getId(),
                    null);
        }
        NavBenchmarkLexicon benchLexicon =
                NavBenchmarkLexicon.load(
                        policy.getBenchmarksLexicon() != null
                                ? policy.getBenchmarksLexicon()
                                : NavBenchmarkLexicon.DEFAULT_RESOURCE);
        List<ChartSeriesSlot> chartSeries = new ArrayList<>();
        chartSeries.add(
                new ChartSeriesSlot(0, ChartSeriesRole.FUND, seriesLabels.get(0), null));
        for (int s = 1; s < seriesLabels.size(); s++) {
            String label = seriesLabels.get(s);
            String key = benchLexicon.resolveBenchmarkKey(label, task.getId());
            chartSeries.add(
                    new ChartSeriesSlot(s, ChartSeriesRole.BENCHMARK, label, key));
        }
        NavChartTimeRange timeRange =
                navChartTimeRangeResolver.resolve(categories, reporting.asOfDate());
        List<String> axisLabels = timeRange.axisLabels();
        List<DimensionSlot> dimensions =
                List.of(
                        new DimensionSlot(
                                0,
                                DimensionSlotRole.CATEGORY,
                                "区间",
                                QueryCondition.dateRange(
                                        "nav_chart",
                                        timeRange.startDate(),
                                        timeRange.endDate())));
        QueryPlanWriteBack writeBack = new QueryPlanWriteBack(0, 0, axisLabels);
        log.debug(
                "nav_line_series task={} range={}~{} granularity={} templatePts={} axisPts={} series={}",
                task.getId(),
                timeRange.startDate(),
                timeRange.endDate(),
                timeRange.granularity(),
                categories.size(),
                axisLabels.size(),
                chartSeries.size());
        return new QueryPlan(
                task.getId(),
                reporting.asOfDate(),
                reporting.asOfQuarter(),
                dimensions,
                writeBack,
                null,
                null,
                null,
                chartSeries,
                timeRange);
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
            p.setPolicyType("nav_line_series");
            p.setBenchmarksLexicon("nav_benchmarks.yaml");
        } else {
            p.setPolicyType("asset_class_allocation");
            p.setCategoryCount(4);
        }
        return p;
    }
}
