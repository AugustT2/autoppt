package com.example.pptrefresh.document;

import com.example.pptrefresh.config.PptRefreshProperties;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TextReplaceMode;
import com.example.pptrefresh.write.TaskWritePayload;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class PptWriteService {

    private final PptRefreshProperties properties;

    public PptWriteService(PptRefreshProperties properties) {
        this.properties = properties;
    }

    public void apply(TaskDefinition task, ResolvedTarget target, TaskWritePayload payload) {
        try {
            switch (task.getType()) {
                case text:
                    applyText(task, target, payload.getText());
                    break;
                case table:
                    applyTable(target.table(), payload.getCells());
                    break;
                case chart:
                    ChartDataWriter.write(
                            target.chart(),
                            payload.getCategories(),
                            payload.getSeriesNames(),
                            payload.getSeriesValues(),
                            properties.chartWriteModeEnum());
                    break;
                default:
                    throw new IllegalStateException("未知任务类型: " + task.getType());
            }
        } catch (RefreshException e) {
            throw e;
        } catch (Exception e) {
            String detail = e.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = e.getClass().getSimpleName();
            }
            throw new RefreshException(
                    FailureStage.TASK_WRITE,
                    "WRITE_FAILED",
                    "写回失败: " + detail,
                    task.getId(),
                    e);
        }
    }

    private void applyText(TaskDefinition task, ResolvedTarget target, String newText) {
        XSLFTextShape text = target.textShape();
        String anchor = task.getAnchorText();
        String original = text.getText();
        if (original == null) {
            original = "";
        }
        String result;
        if (task.getMode() == TextReplaceMode.replace_all) {
            result = newText;
        } else {
            int idx = original.indexOf(anchor);
            if (idx < 0) {
                throw new RefreshException(
                        FailureStage.TASK_WRITE,
                        "ANCHOR_MISSING_AT_WRITE",
                        "写回时锚点不存在",
                        task.getId(),
                        null);
            }
            result = original.substring(0, idx + anchor.length()) + newText;
        }
        TextStylePreserver.setShapeText(text, result);
    }

    private void applyTable(XSLFTable table, List<List<String>> cells) {
        int rows = table.getNumberOfRows();
        int cols = table.getNumberOfColumns();
        if (cells.size() != rows) {
            throw new IllegalArgumentException("cells 行数与表不一致");
        }
        for (int r = 0; r < rows; r++) {
            List<String> row = cells.get(r);
            if (row.size() != cols) {
                throw new IllegalArgumentException("cells 列数与表不一致，行 " + r);
            }
            for (int c = 0; c < cols; c++) {
                setCell(table.getCell(r, c), row.get(c));
            }
        }
    }

    private static void setCell(org.apache.poi.xslf.usermodel.XSLFTableCell cell, String text) {
        TextStylePreserver.setCellText(cell, text);
    }
}
