package com.example.pptrefresh.query.metric;

import com.example.pptrefresh.query.QueryCondition;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Stub：指数/基准类（偏债混合基金指数等）。 */
@Component
public class IndexMetricProvider implements MetricDataProvider {

    public static final String NAME = "index";

    @Override
    public String providerName() {
        return NAME;
    }

    @Override
    public Map<String, String> fetchBatch(
            String fundCode, QueryCondition interval, Collection<String> metricIds) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String metricId : metricIds) {
            Random r = new Random(StubMetricValues.seed(fundCode, "index|" + metricId, interval));
            String value =
                    switch (metricId) {
                        case "bench_return" -> StubMetricValues.formatReturnPct(r);
                        default -> "";
                    };
            out.put(metricId, value);
        }
        return out;
    }
}
