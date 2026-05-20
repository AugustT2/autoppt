package com.example.pptrefresh.query;

/** 表格一行业绩数据（除区间标签外）。 */
public final class PerformanceRowData {

    private final String returnPct;
    private final String peerRank;
    private final String percentile;

    public PerformanceRowData(String returnPct, String peerRank, String percentile) {
        this.returnPct = returnPct;
        this.peerRank = peerRank;
        this.percentile = percentile;
    }

    public String returnPct() {
        return returnPct;
    }

    public String peerRank() {
        return peerRank;
    }

    public String percentile() {
        return percentile;
    }
}
