package com.example.pptrefresh.document;

public final class SlideStructure {

    private final int tableRows;
    private final int tableCols;
    private final int categoryCount;
    private final int seriesCount;

    public SlideStructure(int tableRows, int tableCols, int categoryCount, int seriesCount) {
        this.tableRows = tableRows;
        this.tableCols = tableCols;
        this.categoryCount = categoryCount;
        this.seriesCount = seriesCount;
    }

    public int tableRows() {
        return tableRows;
    }

    public int tableCols() {
        return tableCols;
    }

    public int categoryCount() {
        return categoryCount;
    }

    public int seriesCount() {
        return seriesCount;
    }
}
