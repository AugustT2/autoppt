package com.example.pptrefresh.query;

/**
 * 按 QueryPlan 中的 {@link QueryCondition} 查询业务数据。生产环境对接真实基金数据接口。
 */
public interface QueryPlanDataClient {

    /**
     * 某基金在指定报告季、大类资产上的配置占比（百分点，如 28.5 表示 28.5%）。
     *
     * <p>Stub 返回未归一化权重；{@link QueryPlanDataService#buildAllocationChart} 会按季度对
     * 同一系列内各类资产归一化到合计 100%。生产实现若库表已是最终占比，可在实现内直接返回
     * 百分比并让归一化步骤近似不变（或后续改为跳过归一化）。
     *
     * @param quarter 报告季，如 {@code 2026Q1}
     * @param assetClass 横轴大类标签，如 {@code 股票}、{@code 可转债}
     */
    double fetchAssetClassAllocationPct(String fundCode, String quarter, String assetClass);

    /** 某月末累计收益率（百分点，如 12.0 表示 12%）。 */
    double fetchCumulativeReturnPct(String fundCode, String month, boolean benchmark);

    /** 本基金在指定日/月末的累计收益率（百分点）。 */
    double fetchFundNavReturnPct(String fundCode, String timePoint);

    /** 指定基准在指定日/月末的累计收益率（百分点）。 */
    double fetchBenchmarkNavReturnPct(String fundCode, String benchmarkKey, String timePoint);

    /**
     * 批量查询本基金在 {@link NavChartTimeRange} 内的累计收益序列（一次请求覆盖起止日）。
     *
     * <p>返回 map 的 key：日频 {@code yyyy-MM-dd}，月频 {@code yyyy-MM}；应覆盖区间内各刻度点。
     */
    NavSeriesPoints fetchFundNavReturnsInRange(String fundCode, NavChartTimeRange range);

    /** 批量查询指定基准在同一时间窗内的累计收益序列。 */
    NavSeriesPoints fetchBenchmarkNavReturnsInRange(
            String fundCode, String benchmarkKey, NavChartTimeRange range);
}
