package com.example.pptrefresh.query;

import java.util.List;

public final class ChartSeriesData {

    private final List<String> categories;
    private final List<String> seriesNames;
    private final List<List<Double>> seriesValues;

    public ChartSeriesData(
            List<String> categories, List<String> seriesNames, List<List<Double>> seriesValues) {
        this.categories = List.copyOf(categories);
        this.seriesNames = List.copyOf(seriesNames);
        this.seriesValues = seriesValues.stream().map(List::copyOf).toList();
    }

    public List<String> categories() {
        return categories;
    }

    public List<String> seriesNames() {
        return seriesNames;
    }

    public List<List<Double>> seriesValues() {
        return seriesValues;
    }
}
