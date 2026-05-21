package com.example.pptrefresh.query;

import java.time.LocalDate;
import java.util.List;

/**
 * 折线图取数时间窗：一次批量查询的起止日期 + 写回对齐用的横轴标签。
 *
 * <p>日频：{@link #axisLabels()} 为起止日内全量交易日（与嵌入表一致），与 PPT 横轴显示抽样无关。
 */
public final class NavChartTimeRange {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final NavChartAxisGranularity granularity;
    /** 模板横轴标签顺序，写回时按此对齐 seriesValues。 */
    private final List<String> axisLabels;

    public NavChartTimeRange(
            LocalDate startDate,
            LocalDate endDate,
            NavChartAxisGranularity granularity,
            List<String> axisLabels) {
        if (startDate == null || endDate == null || granularity == null) {
            throw new IllegalArgumentException("nav chart time range 参数不完整");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate 晚于 endDate: " + startDate + " > " + endDate);
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.granularity = granularity;
        this.axisLabels = axisLabels == null ? List.of() : List.copyOf(axisLabels);
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public NavChartAxisGranularity granularity() {
        return granularity;
    }

    public List<String> axisLabels() {
        return axisLabels;
    }
}
