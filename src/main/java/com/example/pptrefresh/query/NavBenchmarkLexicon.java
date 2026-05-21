package com.example.pptrefresh.query;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 折线图基准图例 → benchmarkKey。 */
public final class NavBenchmarkLexicon {

    public static final String DEFAULT_RESOURCE = "/rules/lexicon/nav_benchmarks.yaml";

    private final Map<String, String> benchmarks;

    private NavBenchmarkLexicon(Map<String, String> benchmarks) {
        this.benchmarks = benchmarks;
    }

    @SuppressWarnings("unchecked")
    public static NavBenchmarkLexicon load(String resourcePath) {
        String path =
                resourcePath == null || resourcePath.isBlank()
                        ? DEFAULT_RESOURCE
                        : resourcePath.startsWith("/")
                                ? resourcePath
                                : "/rules/lexicon/" + resourcePath;
        if (!path.endsWith(".yaml")) {
            path = path + ".yaml";
        }
        try (InputStream in = NavBenchmarkLexicon.class.getResourceAsStream(path)) {
            if (in == null) {
                return new NavBenchmarkLexicon(Map.of());
            }
            Object raw = new Yaml().load(in);
            if (!(raw instanceof Map<?, ?> root)) {
                return new NavBenchmarkLexicon(Map.of());
            }
            Object benchObj = root.get("benchmarks");
            if (!(benchObj instanceof Map<?, ?> map)) {
                return new NavBenchmarkLexicon(Map.of());
            }
            Map<String, String> out = new LinkedHashMap<>();
            map.forEach(
                    (k, v) -> {
                        if (k == null || v == null) {
                            return;
                        }
                        String key = String.valueOf(k).trim();
                        String id =
                                v instanceof Map<?, ?> entry
                                        ? String.valueOf(((Map<?, ?>) entry).get("id"))
                                        : String.valueOf(v);
                        out.put(key, id.trim());
                    });
            return new NavBenchmarkLexicon(Collections.unmodifiableMap(out));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load nav benchmark lexicon: " + path, e);
        }
    }

    public String resolveBenchmarkKey(String label, String taskId) {
        if (label == null || label.isBlank()) {
            throw lexiconError("图例名为空", taskId, null);
        }
        String trimmed = label.trim();
        String key = benchmarks.get(trimmed);
        if (key != null) {
            return key;
        }
        String normalized = FundLabelMatcher.normalize(trimmed);
        for (Map.Entry<String, String> e : benchmarks.entrySet()) {
            if (FundLabelMatcher.normalize(e.getKey()).equals(normalized)) {
                return e.getValue();
            }
        }
        throw lexiconError("未识别的基准图例: " + trimmed, taskId, null);
    }

    private static RefreshException lexiconError(String msg, String taskId, Throwable cause) {
        return new RefreshException(
                FailureStage.QUERY_PLAN_BUILD,
                "NAV_BENCHMARK_UNKNOWN",
                msg,
                taskId,
                cause);
    }
}
