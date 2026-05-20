package com.example.pptrefresh.query.metric;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 从 classpath 加载表头文案 → {@link MetricSpec}。 */
public final class MetricCatalog {

    public static final String DEFAULT_RESOURCE = "/rules/lexicon/table_metrics.yaml";

    private final Map<String, MetricSpec> byLabel;

    private MetricCatalog(Map<String, MetricSpec> byLabel) {
        this.byLabel = byLabel;
    }

    @SuppressWarnings("unchecked")
    public static MetricCatalog load(String resourcePath) {
        String path = resourcePath == null || resourcePath.isBlank() ? DEFAULT_RESOURCE : resourcePath;
        if (!path.startsWith("/")) {
            path = "/rules/lexicon/" + path;
        }
        if (!path.endsWith(".yaml")) {
            path = path + ".yaml";
        }
        try (InputStream in = MetricCatalog.class.getResourceAsStream(path)) {
            if (in == null) {
                return new MetricCatalog(Map.of());
            }
            Object raw = new Yaml().load(in);
            if (!(raw instanceof Map<?, ?> root)) {
                return new MetricCatalog(Map.of());
            }
            Object metricsObj = root.get("metrics");
            if (!(metricsObj instanceof Map<?, ?> metricsMap)) {
                return new MetricCatalog(Map.of());
            }
            Map<String, MetricSpec> out = new LinkedHashMap<>();
            metricsMap.forEach(
                    (labelKey, specObj) -> {
                        if (!(specObj instanceof Map<?, ?> specMap)) {
                            return;
                        }
                        Object id = specMap.get("id");
                        Object provider = specMap.get("provider");
                        if (id == null || provider == null) {
                            return;
                        }
                        out.put(
                                String.valueOf(labelKey).trim(),
                                new MetricSpec(
                                        String.valueOf(id).trim(),
                                        String.valueOf(provider).trim()));
                    });
            return new MetricCatalog(Collections.unmodifiableMap(out));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load metric catalog: " + path, e);
        }
    }

    public ResolvedMetric resolve(String displayLabel) {
        if (displayLabel == null) {
            return null;
        }
        String trimmed = displayLabel.trim();
        MetricSpec spec = byLabel.get(trimmed);
        if (spec == null) {
            spec = byLabel.get(normalize(trimmed));
        }
        if (spec == null) {
            return null;
        }
        return new ResolvedMetric(trimmed, spec.metricId(), spec.providerName());
    }

    private static String normalize(String label) {
        return label.replaceAll("\\s+", "");
    }
}
