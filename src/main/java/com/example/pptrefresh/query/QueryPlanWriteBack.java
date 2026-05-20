package com.example.pptrefresh.query;

import java.util.List;

public final class QueryPlanWriteBack {

    private final int tableRows;
    private final int tableCols;
    private final List<String> categoryLabels;

    public QueryPlanWriteBack(int tableRows, int tableCols, List<String> categoryLabels) {
        this.tableRows = tableRows;
        this.tableCols = tableCols;
        this.categoryLabels = categoryLabels;
    }

    public int tableRows() {
        return tableRows;
    }

    public int tableCols() {
        return tableCols;
    }

    public List<String> categoryLabels() {
        return categoryLabels;
    }
}
