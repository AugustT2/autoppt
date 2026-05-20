package com.example.pptrefresh.rules;

public class DimensionPolicy {

    private String policyType;
    /** 区间标签词表（classpath 路径，如 /rules/lexicon/fund_performance_rows.yaml） */
    private String lexicon;
    private Integer categoryCount;
    private Integer monthPointCount;

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

    public Integer getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(Integer categoryCount) {
        this.categoryCount = categoryCount;
    }

    public Integer getMonthPointCount() {
        return monthPointCount;
    }

    public void setMonthPointCount(Integer monthPointCount) {
        this.monthPointCount = monthPointCount;
    }
}
