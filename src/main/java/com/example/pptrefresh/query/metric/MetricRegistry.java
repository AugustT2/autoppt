package com.example.pptrefresh.query.metric;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MetricRegistry {

    private final Map<String, MetricDataProvider> byName;

    public MetricRegistry(List<MetricDataProvider> providers) {
        Map<String, MetricDataProvider> map = new LinkedHashMap<>();
        for (MetricDataProvider p : providers) {
            map.put(p.providerName(), p);
        }
        this.byName = Map.copyOf(map);
    }

    public MetricDataProvider require(String providerName) {
        MetricDataProvider p = byName.get(providerName);
        if (p == null) {
            throw new IllegalArgumentException("未注册的 metric provider: " + providerName);
        }
        return p;
    }
}
