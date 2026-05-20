package com.example.pptrefresh.query.metric;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.query.QueryCondition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 按区间批量拉取表格指标（provider 分组 + batch）。 */
@Service
public class TableMetricFetchService {

    private final MetricRegistry registry;

    public TableMetricFetchService(MetricRegistry registry) {
        this.registry = registry;
    }

    //把 PPT 表头上的中文指标名，翻译成程序内部能用的 metricId + providerName，并保留原来的展示名写回表格。
    public List<ResolvedMetric> resolveLabels(MetricCatalog catalog, List<String> displayLabels) {
        List<ResolvedMetric> out = new ArrayList<>();
        for (String label : displayLabels) {
            ResolvedMetric m = catalog.resolve(label);
            if (m == null) {
                throw new RefreshException(
                        FailureStage.QUERY_PLAN_BUILD,
                        "METRIC_CATALOG_UNKNOWN",
                        "指标词表无法识别: " + label,
                        null,
                        null);
            }
            out.add(m);
        }
        return out;
    }

    /**
     * 某一区间下，为本表涉及的所有指标取值。
     *
     * @return metricId → 单元格文案
     */
    public Map<String, String> fetchForInterval(
            String fundCode, QueryCondition interval, List<ResolvedMetric> metrics) {
        Map<String, Set<String>> idsByProvider = new LinkedHashMap<>();
        for (ResolvedMetric m : metrics) {
            idsByProvider
                    .computeIfAbsent(m.providerName(), k -> new LinkedHashSet<>())
                    .add(m.metricId());
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : idsByProvider.entrySet()) {
            MetricDataProvider provider = registry.require(e.getKey());
            Map<String, String> batch =
                    provider.fetchBatch(fundCode, interval, List.copyOf(e.getValue()));
            values.putAll(batch);
        }
        return values;
    }
}
