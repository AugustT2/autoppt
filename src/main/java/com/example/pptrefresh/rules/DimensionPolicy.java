package com.example.pptrefresh.rules;

public class DimensionPolicy {

    private String policyType;
    /** ROW=标签在某列（一行一区）；COLUMN=标签在某行；AUTO=词表探测。 */
    private String labelAxis;
    /** 标签所在列号（ROW）或行号（COLUMN），默认 0。 */
    private Integer labelIndex;
    /** 跳过的表头行数（ROW）或列数（COLUMN），默认 1。 */
    private Integer headerSpan;
    /** @deprecated 请用 labelIndex + labelAxis=ROW */
    private Integer intervalColumn;
    /** @deprecated 请用 headerSpan + labelAxis=ROW */
    private Integer headerRows;
    private String lexicon;
    private Integer categoryCount;
    private Integer monthPointCount;

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getLabelAxis() {
        return labelAxis;
    }

    public void setLabelAxis(String labelAxis) {
        this.labelAxis = labelAxis;
    }

    public Integer getLabelIndex() {
        return labelIndex;
    }

    public void setLabelIndex(Integer labelIndex) {
        this.labelIndex = labelIndex;
    }

    public Integer getHeaderSpan() {
        return headerSpan;
    }

    public void setHeaderSpan(Integer headerSpan) {
        this.headerSpan = headerSpan;
    }

    public Integer getIntervalColumn() {
        return intervalColumn;
    }

    public void setIntervalColumn(Integer intervalColumn) {
        this.intervalColumn = intervalColumn;
    }

    public Integer getHeaderRows() {
        return headerRows;
    }

    public void setHeaderRows(Integer headerRows) {
        this.headerRows = headerRows;
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
