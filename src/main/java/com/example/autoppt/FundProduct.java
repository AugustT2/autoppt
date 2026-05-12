package com.example.autoppt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 一只基金在产品页上需要替换的文本、表格与嵌入图表数据。
 */
public final class FundProduct {

    private final String titleLine;
    private final String sectionFundLabel;
    private final List<String> profileLines;
    private final String sectionRankLabel;
    private final List<String> tableHeaders;
    private final List<List<String>> tableRows;
    private final String sectionAllocLabel;
    private final String sectionNavLabel;
    private final FundCharts charts;

    public FundProduct(
            String titleLine,
            String sectionFundLabel,
            List<String> profileLines,
            String sectionRankLabel,
            List<String> tableHeaders,
            List<List<String>> tableRows,
            String sectionAllocLabel,
            String sectionNavLabel,
            FundCharts charts) {
        this.titleLine = titleLine;
        this.sectionFundLabel = sectionFundLabel;
        this.profileLines = List.copyOf(profileLines);
        this.sectionRankLabel = sectionRankLabel;
        this.tableHeaders = List.copyOf(tableHeaders);
        this.tableRows =
                tableRows.stream()
                        .map(ArrayList::new)
                        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        this.sectionAllocLabel = sectionAllocLabel;
        this.sectionNavLabel = sectionNavLabel;
        this.charts = charts;
    }

    public String titleLine() {
        return titleLine;
    }

    public String sectionFundLabel() {
        return sectionFundLabel;
    }

    public List<String> profileLines() {
        return profileLines;
    }

    public String sectionRankLabel() {
        return sectionRankLabel;
    }

    public List<String> tableHeaders() {
        return tableHeaders;
    }

    public List<List<String>> tableRows() {
        return tableRows;
    }

    public String sectionAllocLabel() {
        return sectionAllocLabel;
    }

    public String sectionNavLabel() {
        return sectionNavLabel;
    }

    public FundCharts charts() {
        return charts;
    }
}
