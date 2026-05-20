package com.example.pptrefresh.query.metric;

/** 词表中的一条指标定义（内部 id + 取数 provider 名）。 */
public record MetricSpec(String metricId, String providerName) {}
