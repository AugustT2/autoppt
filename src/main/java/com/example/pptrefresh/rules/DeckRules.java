package com.example.pptrefresh.rules;

import java.util.ArrayList;
import java.util.List;

public class DeckRules {

    private String deckType;
    private String version;
    private List<String> timeRules = new ArrayList<>();
    private ProductNameResolution productNameResolution;
    private List<TaskDefinition> tasks = new ArrayList<>();

    public String getDeckType() {
        return deckType;
    }

    public void setDeckType(String deckType) {
        this.deckType = deckType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getTimeRules() {
        return timeRules;
    }

    public void setTimeRules(List<String> timeRules) {
        this.timeRules = timeRules;
    }

    public ProductNameResolution getProductNameResolution() {
        return productNameResolution;
    }

    public void setProductNameResolution(ProductNameResolution productNameResolution) {
        this.productNameResolution = productNameResolution;
    }

    public List<TaskDefinition> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDefinition> tasks) {
        this.tasks = tasks;
    }
}
