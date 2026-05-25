package com.example.pptrefresh.query.metric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetricCatalogTest {

    private final MetricCatalog catalog = MetricCatalog.load(MetricCatalog.DEFAULT_RESOURCE);

    @Test
    void resolvesPerformanceTableRowLabels() {
        assertMetric("累计收益", "return_pct", "performance");
        assertMetric("年化收益", "return_annualized", "performance");
        assertMetric("收益排名", "peer_rank", "performance");
        assertMetric("二级债基指数（年化）", "bench_return", "index");
        assertMetric("夏普比率", "sharpe_ratio", "risk");
        assertMetric("最大回撤", "max_drawdown", "risk");
    }

    private void assertMetric(String label, String id, String provider) {
        ResolvedMetric m = catalog.resolve(label);
        assertNotNull(m, label);
        assertEquals(id, m.metricId());
        assertEquals(provider, m.providerName());
    }
}
