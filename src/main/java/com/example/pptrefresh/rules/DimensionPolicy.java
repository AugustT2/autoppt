package com.example.pptrefresh.rules;

import java.util.List;

public class DimensionPolicy {

    private String policyType;
    /** 区间标签词表（classpath 路径，如 /rules/lexicon/fund_performance_rows.yaml） */
    private String lexicon;
    /** 表格指标词表（classpath 路径，如 /rules/lexicon/table_metrics.yaml） */
    private String metricsLexicon;
    /** 资产配置图：横轴大类（股票、可转债…） */
    private List<String> chartCategories;
    /** 资产配置图：系列名（2025q2、2025q3…） */
    private List<String> chartSeriesNames;
    /** 累计收益折线图：基准图例词表（classpath，如 nav_benchmarks.yaml） */
    private String benchmarksLexicon;
    private Integer categoryCount;

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getLexicon() {
        return lexicon;
    }

    public void setLexicon(String lexicon) {
        this.lexicon = lexicon;
    }

    public String getMetricsLexicon() {
        return metricsLexicon;
    }

    public void setMetricsLexicon(String metricsLexicon) {
        this.metricsLexicon = metricsLexicon;
    }

    public List<String> getChartCategories() {
        return chartCategories;
    }

    public void setChartCategories(List<String> chartCategories) {
        this.chartCategories = chartCategories;
    }

    public List<String> getChartSeriesNames() {
        return chartSeriesNames;
    }

    public void setChartSeriesNames(List<String> chartSeriesNames) {
        this.chartSeriesNames = chartSeriesNames;
    }

    public String getBenchmarksLexicon() {
        return benchmarksLexicon;
    }

    public void setBenchmarksLexicon(String benchmarksLexicon) {
        this.benchmarksLexicon = benchmarksLexicon;
    }

    public Integer getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(Integer categoryCount) {
        this.categoryCount = categoryCount;
    }

}
