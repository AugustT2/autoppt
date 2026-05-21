package com.example.pptrefresh.query;

/** 折线图单条系列：模板图例 + 查数键。 */
public final class ChartSeriesSlot {

    private final int index;
    private final ChartSeriesRole role;
    private final String label;
    /** FUND 为 null；BENCHMARK 为词表解析出的 benchmarkKey。 */
    private final String queryKey;

    public ChartSeriesSlot(int index, ChartSeriesRole role, String label, String queryKey) {
        this.index = index;
        this.role = role;
        this.label = label;
        this.queryKey = queryKey;
    }

    public int index() {
        return index;
    }

    public ChartSeriesRole role() {
        return role;
    }

    public String label() {
        return label;
    }

    public String queryKey() {
        return queryKey;
    }
}
