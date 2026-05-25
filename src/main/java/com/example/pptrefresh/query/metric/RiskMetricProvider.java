package com.example.pptrefresh.query.metric;

import com.example.pptrefresh.query.QueryCondition;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Stub：风险类（风险回报、最大回撤）。 */
@Component
public class RiskMetricProvider implements MetricDataProvider {

    public static final String NAME = "risk";

    @Override
    public String providerName() {
        return NAME;
    }

    @Override
    public Map<String, String> fetchBatch(
            String fundCode, QueryCondition interval, Collection<String> metricIds) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String metricId : metricIds) {
            Random r = new Random(StubMetricValues.seed(fundCode, metricId, interval));
            String value =
                    switch (metricId) {
                        case "risk_return" -> StubMetricValues.formatPercentile(r);
                        case "max_drawdown" -> StubMetricValues.formatDrawdown(r);
                        case "sharpe_ratio" -> StubMetricValues.formatSharpe(r);
                        default -> "";
                    };
            out.put(metricId, value);
        }
        return out;
    }
}
