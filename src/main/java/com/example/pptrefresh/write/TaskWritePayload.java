package com.example.pptrefresh.write;

import com.example.pptrefresh.rules.TaskType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskWritePayload {

    private TaskType type;
    private String text;
    private List<List<String>> cells;
    private List<String> categories;
    private List<String> seriesNames;
    private List<List<Double>> seriesValues;

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<List<String>> getCells() {
        return cells;
    }

    public void setCells(List<List<String>> cells) {
        this.cells = cells;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<String> getSeriesNames() {
        return seriesNames;
    }

    public void setSeriesNames(List<String> seriesNames) {
        this.seriesNames = seriesNames;
    }

    public List<List<Double>> getSeriesValues() {
        return seriesValues;
    }

    public void setSeriesValues(List<List<Double>> seriesValues) {
        this.seriesValues = seriesValues;
    }
}
