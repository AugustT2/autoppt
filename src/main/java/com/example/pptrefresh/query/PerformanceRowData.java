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

    /** 按表头指标名取值（兼容常见列名变体）。 */
    public String metricValue(String metricName) {
        if (metricName == null) {
            return "";
        }
        String m = metricName.trim();
        if (m.contains("收益率") && !m.contains("排名") && !m.contains("指数")) {
            return returnPct;
        }
        if (m.contains("排名")) {
            return peerRank;
        }
        if (m.contains("分位")) {
            return percentile;
        }
        if (m.contains("指数")) {
            return returnPct;
        }
        if (m.contains("风险回报")) {
            return percentile;
        }
        if (m.contains("回撤")) {
            return peerRank;
        }
        return returnPct;
    }
}
