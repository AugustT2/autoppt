package com.example.pptrefresh.document;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TargetResolver {

    public ResolvedTarget resolve(XMLSlideShow ppt, int slideIndexBase, TaskDefinition task) {
        int slideIdx = slideIndexBase + task.getSlideIndex();
        if (slideIdx < 0 || slideIdx >= ppt.getSlides().size()) {
            throw new RefreshException(
                    FailureStage.TASK_RESOLVE_TARGET,
                    "SLIDE_OUT_OF_RANGE",
                    "幻灯片索引越界: " + slideIdx,
                    task.getId(),
                    null);
        }
        XSLFSlide slide = ppt.getSlides().get(slideIdx);
        switch (task.getType()) {
            case text:
                return resolveText(slide, task);
            case table:
                return resolveTable(slide, task);
            case chart:
                return resolveChart(slide, task);
            default:
                throw new RefreshException(
                        FailureStage.TASK_RESOLVE_TARGET,
                        "UNKNOWN_TASK_TYPE",
                        "未知任务类型",
                        task.getId(),
                        null);
        }
    }

    private ResolvedTarget resolveText(XSLFSlide slide, TaskDefinition task) {
        List<XSLFTextShape> matches = new ArrayList<>();
        ShapeWalker.walkDepthFirst(
                slide,
                shape -> {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape text = (XSLFTextShape) shape;
                        String content = text.getText();
                        if (content != null && content.contains(task.getAnchorText())) {
                            matches.add(text);
                        }
                    }
                });
        if (matches.isEmpty()) {
            throw new RefreshException(
                    FailureStage.TASK_RESOLVE_TARGET,
                    "ANCHOR_NOT_FOUND",
                    "未找到锚点: " + task.getAnchorText(),
                    task.getId(),
                    null);
        }
        if (matches.size() > 1) {
            throw new RefreshException(
                    FailureStage.TASK_RESOLVE_TARGET,
                    "ANCHOR_NOT_UNIQUE",
                    "锚点不唯一: " + task.getAnchorText() + "，命中 " + matches.size(),
                    task.getId(),
                    null);
        }
        return new ResolvedTarget(
                TaskType.text, matches.get(0), task.getAnchorText(), null, null, null);
    }

    private ResolvedTarget resolveTable(XSLFSlide slide, TaskDefinition task) {
        int ordinal = task.getTableOrdinal();
        int seen = 0;
        List<XSLFShape> shapes = ShapeWalker.collectDepthFirst(slide);
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFTable) {
                XSLFTable table = (XSLFTable) shape;
                seen++;
                if (seen == ordinal) {
                    SlideStructure structure =
                            new SlideStructure(table.getNumberOfRows(), table.getNumberOfColumns(), 0, 0);
                    return new ResolvedTarget(TaskType.table, null, null, table, null, structure);
                }
            }
        }
        throw notFound(task, "表格", ordinal);
    }

    private ResolvedTarget resolveChart(XSLFSlide slide, TaskDefinition task) {
        int ordinal = task.getChartOrdinal();
        int seen = 0;
        List<XSLFShape> shapes = ShapeWalker.collectDepthFirst(slide);
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFGraphicFrame) {
                XSLFGraphicFrame frame = (XSLFGraphicFrame) shape;
                if (!frame.hasChart()) {
                    continue;
                }
                seen++;
                if (seen == ordinal) {
                    XSLFChart chart = frame.getChart();
                    int categories = 0;
                    int series = 0;
                    try {
                        if (chart.getCTChart() != null
                                && chart.getCTChart().getPlotArea() != null
                                && !chart.getCTChart().getPlotArea().getBarChartList().isEmpty()) {
                            org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart bar =
                                    chart.getCTChart().getPlotArea().getBarChartList().get(0);
                            series = bar.getSerList().size();
                        }
                    } catch (Exception ignored) {
                        // 结构预读失败时写回阶段再校验
                    }
                    SlideStructure structure = new SlideStructure(0, 0, categories, series);
                    return new ResolvedTarget(TaskType.chart, null, null, null, chart, structure);
                }
            }
        }
        throw notFound(task, "图表", ordinal);
    }

    private RefreshException notFound(TaskDefinition task, String kind, int ordinal) {
        return new RefreshException(
                FailureStage.TASK_RESOLVE_TARGET,
                "TARGET_NOT_FOUND",
                "未找到第 " + ordinal + " 个" + kind,
                task.getId(),
                null);
    }
}
