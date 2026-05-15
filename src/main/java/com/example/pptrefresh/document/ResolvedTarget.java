package com.example.pptrefresh.document;

import com.example.pptrefresh.rules.TaskType;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

public final class ResolvedTarget {

    private final TaskType type;
    private final XSLFTextShape textShape;
    private final String anchorText;
    private final XSLFTable table;
    private final XSLFChart chart;
    private final SlideStructure structure;

    public ResolvedTarget(
            TaskType type,
            XSLFTextShape textShape,
            String anchorText,
            XSLFTable table,
            XSLFChart chart,
            SlideStructure structure) {
        this.type = type;
        this.textShape = textShape;
        this.anchorText = anchorText;
        this.table = table;
        this.chart = chart;
        this.structure = structure;
    }

    public TaskType type() {
        return type;
    }

    public XSLFTextShape textShape() {
        return textShape;
    }

    public String anchorText() {
        return anchorText;
    }

    public XSLFTable table() {
        return table;
    }

    public XSLFChart chart() {
        return chart;
    }

    public SlideStructure structure() {
        return structure;
    }
}
