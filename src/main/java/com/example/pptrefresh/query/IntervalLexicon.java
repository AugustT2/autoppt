package com.example.pptrefresh.query;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 从 classpath 加载区间标签 → 解析策略。 */
public final class IntervalLexicon {

    private final Map<String, String> labels;

    private IntervalLexicon(Map<String, String> labels) {
        this.labels = labels;
    }

    @SuppressWarnings("unchecked")
    public static IntervalLexicon load(String resourcePath) {
        try (InputStream in = IntervalLexicon.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return new IntervalLexicon(Map.of());
            }
            Object raw = new Yaml().load(in);
            if (!(raw instanceof Map)) {
                return new IntervalLexicon(Map.of());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) raw;
            Object labelsObj = root.get("labels");
            if (!(labelsObj instanceof Map)) {
                return new IntervalLexicon(Map.of());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) labelsObj;
            Map<String, String> out = new LinkedHashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), String.valueOf(v)));
            return new IntervalLexicon(Collections.unmodifiableMap(out));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load lexicon: " + resourcePath, e);
        }
    }

    public String resolveKind(String label) {
        if (label == null) {
            return null;
        }
        String trimmed = label.trim();
        String kind = labels.get(trimmed);
        if (kind != null) {
            return kind;
        }
        return labels.get(normalize(label));
    }

    private static String normalize(String label) {
        return label.replaceAll("\\s+", "");
    }

    public Map<String, String> labels() {
        return labels;
    }
}
