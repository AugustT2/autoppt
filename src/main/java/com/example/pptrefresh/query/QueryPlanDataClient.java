package com.example.pptrefresh.query;

/**
 * 按 QueryPlan 中的 {@link QueryCondition} 查询业务数据。生产环境对接真实基金数据接口。
 */
public interface QueryPlanDataClient {

    PerformanceRowData fetchPerformanceRow(String fundCode, QueryCondition condition);

    /** 某季度资产配置（股票/债券/现金及其他，百分比）。 */
    double[] fetchAllocationPercents(String fundCode, String quarter);

    /** 某月末累计收益率（百分点，如 12.0 表示 12%）。 */
    double fetchCumulativeReturnPct(String fundCode, String month, boolean benchmark);
}
