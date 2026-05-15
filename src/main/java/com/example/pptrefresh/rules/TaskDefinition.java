package com.example.pptrefresh.rules;

public class TaskDefinition {

    private String id;
    private TaskType type;
    private int slideIndex;
    private String anchorText;
    private TextReplaceMode mode;
    private Integer tableOrdinal;
    private Integer chartOrdinal;
    private String intent;
    private String hints;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public int getSlideIndex() {
        return slideIndex;
    }

    public void setSlideIndex(int slideIndex) {
        this.slideIndex = slideIndex;
    }

    public String getAnchorText() {
        return anchorText;
    }

    public void setAnchorText(String anchorText) {
        this.anchorText = anchorText;
    }

    public TextReplaceMode getMode() {
        return mode;
    }

    public void setMode(TextReplaceMode mode) {
        this.mode = mode;
    }

    public Integer getTableOrdinal() {
        return tableOrdinal;
    }

    public void setTableOrdinal(Integer tableOrdinal) {
        this.tableOrdinal = tableOrdinal;
    }

    public Integer getChartOrdinal() {
        return chartOrdinal;
    }

    public void setChartOrdinal(Integer chartOrdinal) {
        this.chartOrdinal = chartOrdinal;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getHints() {
        return hints;
    }

    public void setHints(String hints) {
        this.hints = hints;
    }
}
