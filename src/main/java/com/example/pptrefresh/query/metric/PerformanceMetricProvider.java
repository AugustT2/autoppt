package com.example.pptrefresh.query.metric;

import com.example.pptrefresh.query.QueryCondition;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Stub：业绩类（收益率、排名）。 */
@Component
public class PerformanceMetricProvider implements MetricDataProvider {

    public static final String NAME = "performance";

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
                        case "return_pct" -> StubMetricValues.formatReturnPct(r);
                        case "peer_rank" -> StubMetricValues.formatPeerRank(r);
                        default -> "";
                    };
            out.put(metricId, value);
        }
        return out;
    }
}
