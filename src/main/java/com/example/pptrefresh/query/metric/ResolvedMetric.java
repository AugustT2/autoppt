package com.example.pptrefresh.query.metric;

/** PPT 表头解析后的指标（展示名 + 词表规格）。 */
public record ResolvedMetric(String displayLabel, String metricId, String providerName) {}
