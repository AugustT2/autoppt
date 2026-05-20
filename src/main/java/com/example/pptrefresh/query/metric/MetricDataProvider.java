package com.example.pptrefresh.query.metric;

import com.example.pptrefresh.query.QueryCondition;

import java.util.Collection;
import java.util.Map;

/**
 * 按业务域对接表格指标 API（同一 provider 可覆盖多个 metricId）。
 * 生产环境为每个 provider 提供独立实现类替换 Stub。
 */
public interface MetricDataProvider {

    /** 与 table_metrics.yaml 中 provider 字段一致。 */
    String providerName();

    /**
     * 一次查询返回多个指标（同一区间、同一后端时优先 batch，减少 HTTP 次数）。
     *
     * @param metricIds 本 provider 负责的 id 子集
     * @return metricId → 单元格展示文案
     */
    Map<String, String> fetchBatch(
            String fundCode, QueryCondition interval, Collection<String> metricIds);

    default String fetch(String fundCode, QueryCondition interval, String metricId) {
        Map<String, String> batch = fetchBatch(fundCode, interval, java.util.List.of(metricId));
        return batch.getOrDefault(metricId, "");
    }
}
