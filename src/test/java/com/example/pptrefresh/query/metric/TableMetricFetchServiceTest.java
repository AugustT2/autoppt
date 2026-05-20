package com.example.pptrefresh.query.metric;

import com.example.pptrefresh.query.QueryCondition;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TableMetricFetchServiceTest {

    @Test
    void fetchForIntervalGroupsByProvider() {
        MetricRegistry registry =
                new MetricRegistry(
                        List.of(
                                new PerformanceMetricProvider(),
                                new RiskMetricProvider(),
                                new IndexMetricProvider()));
        TableMetricFetchService fetch = new TableMetricFetchService(registry);
        MetricCatalog catalog = MetricCatalog.load(MetricCatalog.DEFAULT_RESOURCE);
        List<ResolvedMetric> metrics =
                fetch.resolveLabels(
                        catalog,
                        List.of("收益率", "业绩排名", "最大回撤", "偏债混合基金指数"));
        QueryCondition interval =
                QueryCondition.dateRange(
                        "过去一年", LocalDate.of(2024, 5, 1), LocalDate.of(2025, 5, 1));

        Map<String, String> values =
                fetch.fetchForInterval("F001", interval, metrics);

        assertEquals(4, values.size());
        assertFalse(values.get("return_pct").isEmpty());
        assertFalse(values.get("peer_rank").isEmpty());
        assertFalse(values.get("max_drawdown").isEmpty());
        assertFalse(values.get("bench_return").isEmpty());
    }
}
