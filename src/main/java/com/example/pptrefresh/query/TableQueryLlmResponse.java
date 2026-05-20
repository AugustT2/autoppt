package com.example.pptrefresh.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class TableQueryLlmResponse {

    private List<String> intervalLabels;
    private List<String> metrics;
    private String intervalAxis;

    public List<String> getIntervalLabels() {
        return intervalLabels;
    }

    public void setIntervalLabels(List<String> intervalLabels) {
        this.intervalLabels = intervalLabels;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public String getIntervalAxis() {
        return intervalAxis;
    }

    public void setIntervalAxis(String intervalAxis) {
        this.intervalAxis = intervalAxis;
    }
}
