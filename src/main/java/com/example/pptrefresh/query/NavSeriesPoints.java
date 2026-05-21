package com.example.pptrefresh.query;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 批量净值/累计收益序列：key 为横轴标签（yyyy-MM-dd 或 yyyy-MM）。 */
public final class NavSeriesPoints {

    private final Map<String, Double> byLabel;

    private NavSeriesPoints(Map<String, Double> byLabel) {
        this.byLabel = Collections.unmodifiableMap(new LinkedHashMap<>(byLabel));
    }

    public static NavSeriesPoints of(Map<String, Double> byLabel) {
        return new NavSeriesPoints(byLabel == null ? Map.of() : byLabel);
    }

    public Map<String, Double> byLabel() {
        return byLabel;
    }

    public double require(String axisLabel, String taskId, String seriesLabel) {
        Double v = byLabel.get(axisLabel);
        if (v == null) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "NAV_POINT_MISSING",
                    "批量序列缺少横轴点「"
                            + axisLabel
                            + "」系列="
                            + seriesLabel,
                    taskId,
                    null);
        }
        return v;
    }
}
