package com.example.pptrefresh.sample;

import java.util.List;

/** 偏债混-M1 样例页演示数据（基金业绩说明四象限版），供 Stub、Demo Tool 与样例 pptx 对齐。 */
public final class ZhongOuSampleData {

    /** 「收益率排名」表：1 表头 + 3 行数据，4 列。 */
    public static final List<List<String>> YIELD_RANKING_CELLS =
            List.of(
                    List.of("区间", "收益率", "同类排名", "分位数"),
                    List.of("近一年", "+65.8%", "2 / 856", "前 43%"),
                    List.of("近两年", "+34.6%", "344 / 812", "前 44%"),
                    List.of("近三年", "+66.4%", "322 / 897", "前 45%"));

    /** 资产配置柱图：横轴=季度（categories），系列=资产类别（与模板嵌入表行列一致）。 */
    public static final List<String> ALLOCATION_CATEGORIES =
            List.of("2024Q2", "2024Q3", "2024Q4", "2025Q1");

    public static final List<String> ALLOCATION_SERIES_NAMES = List.of("股票", "债券", "现金及其他");

    public static final List<List<Double>> ALLOCATION_SERIES_VALUES =
            List.of(
                    List.of(68.2, 71.5, 65.8, 72.4),
                    List.of(33.5, 21.8, 45.1, 22.6),
                    List.of(9.3, 8.7, 10.1, 3.0));

    private ZhongOuSampleData() {}
}
