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
    /**
     * 可选。配置则进入流水线模式（只注册该 Tool）；不配置则 Agent 模式（注册全部 Tool，由模型按 intent 选择）。
     */
    private String tool;
    /** 可选：deck 特例说明（非工具参数列表）。 */
    private String hints;
    /** 可选：表格/图表维度与查询条件组装策略。 */
    private DimensionPolicy dimensionPolicy;
    /**
     * {@link TextReplaceMode#replace_labeled_number} 时必填，如「最新规模」；只替换该标签后的数字。
     */
    private String fieldLabel;

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

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getHints() {
        return hints;
    }

    public void setHints(String hints) {
        this.hints = hints;
    }

    public DimensionPolicy getDimensionPolicy() {
        return dimensionPolicy;
    }

    public void setDimensionPolicy(DimensionPolicy dimensionPolicy) {
        this.dimensionPolicy = dimensionPolicy;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }
}
