package com.example.autoppt;

import java.util.Arrays;

/**
 * 嵌入 PPT 中两个命名图表的数据（与模板中系列数量一致：柱状 2 条、折线 2 条）。
 */
public final class FundCharts {

    private final String[] allocCategories;
    private final String[] allocSeriesNames;
    private final double[][] allocSeriesValues;
    private final String[] navCategories;
    private final String[] navSeriesNames;
    private final double[][] navSeriesValues;

    public FundCharts(
            String[] allocCategories,
            String[] allocSeriesNames,
            double[][] allocSeriesValues,
            String[] navCategories,
            String[] navSeriesNames,
            double[][] navSeriesValues) {
        this.allocCategories = allocCategories.clone();
        this.allocSeriesNames = allocSeriesNames.clone();
        this.allocSeriesValues = deepCopy(allocSeriesValues);
        this.navCategories = navCategories.clone();
        this.navSeriesNames = navSeriesNames.clone();
        this.navSeriesValues = deepCopy(navSeriesValues);
    }

    private static double[][] deepCopy(double[][] src) {
        double[][] out = new double[src.length][];
        for (int i = 0; i < src.length; i++) {
            out[i] = src[i].clone();
        }
        return out;
    }

    public String[] allocCategories() {
        return allocCategories.clone();
    }

    public String[] allocSeriesNames() {
        return allocSeriesNames.clone();
    }

    public double[][] allocSeriesValues() {
        return deepCopy(allocSeriesValues);
    }

    public String[] navCategories() {
        return navCategories.clone();
    }

    public String[] navSeriesNames() {
        return navSeriesNames.clone();
    }

    public double[][] navSeriesValues() {
        return deepCopy(navSeriesValues);
    }

    @Override
    public String toString() {
        return "FundCharts{alloc="
                + Arrays.toString(allocCategories)
                + ", nav="
                + Arrays.toString(navCategories)
                + "}";
    }
}
