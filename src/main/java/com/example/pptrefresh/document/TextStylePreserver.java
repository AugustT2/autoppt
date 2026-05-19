package com.example.pptrefresh.document;

import org.apache.poi.sl.usermodel.ColorStyle;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.awt.Color;
import java.util.List;

/**
 * 写回时保留模板已有字体样式。优先就地改首段/首 Run，避免 {@code clearText} 破坏 txBody 结构导致 PowerPoint 修复。
 */
final class TextStylePreserver {

    private TextStylePreserver() {}

    static void setShapeText(XSLFTextShape shape, String text) {
        String value = text == null ? "" : text;
        RunStyle style = captureFirstRunStyle(shape);
        if (value.indexOf('\n') < 0 && value.indexOf('\r') < 0 && replaceSingleRun(shape, value, style)) {
            return;
        }
        if (replaceMultiParagraph(shape, value, style)) {
            return;
        }
        shape.clearText();
        XSLFTextRun run = shape.setText(value);
        applyRunStyle(run, style);
    }

    static void setCellText(XSLFTableCell cell, String text) {
        String value = text == null ? "" : text;
        RunStyle style = captureFirstRunStyle(cell);
        if (updateFirstRunOnly(cell, value, style)) {
            return;
        }
        cell.setText(value);
        for (XSLFTextParagraph p : cell.getTextParagraphs()) {
            for (XSLFTextRun run : p.getTextRuns()) {
                applyRunStyle(run, style);
            }
        }
    }

    private static boolean replaceSingleRun(XSLFTextShape shape, String text, RunStyle style) {
        List<XSLFTextParagraph> paragraphs = shape.getTextParagraphs();
        if (paragraphs.size() != 1) {
            return false;
        }
        List<XSLFTextRun> runs = paragraphs.get(0).getTextRuns();
        if (runs.size() != 1) {
            return false;
        }
        XSLFTextRun run = runs.get(0);
        run.setText(text);
        applyRunStyle(run, style);
        return true;
    }

    /**
     * 多行文本按现有段落数逐段写入，保留各段 defRPr/Run 样式。
     *
     * <p>不可用首段样式（如基金名称段加粗）覆盖后续段，否则 fund_meta 等整块会被误加粗。
     */
    private static boolean replaceMultiParagraph(XSLFTextShape shape, String text, RunStyle unused) {
        String[] lines = text.split("\\r?\\n", -1);
        List<XSLFTextParagraph> paragraphs = shape.getTextParagraphs();
        if (paragraphs.isEmpty() || lines.length != paragraphs.size()) {
            return false;
        }
        for (int i = 0; i < lines.length; i++) {
            List<XSLFTextRun> runs = paragraphs.get(i).getTextRuns();
            if (runs.isEmpty()) {
                return false;
            }
            XSLFTextRun run = runs.get(0);
            RunStyle paragraphStyle = RunStyle.from(run);
            run.setText(lines[i]);
            applyRunStyle(run, paragraphStyle);
            for (int r = 1; r < runs.size(); r++) {
                runs.get(r).setText("");
            }
        }
        return true;
    }

    private static boolean updateFirstRunOnly(XSLFTableCell cell, String text, RunStyle style) {
        if (cell.getTextParagraphs().isEmpty()) {
            return false;
        }
        XSLFTextParagraph paragraph = cell.getTextParagraphs().get(0);
        if (paragraph.getTextRuns().isEmpty()) {
            return false;
        }
        XSLFTextRun run = paragraph.getTextRuns().get(0);
        run.setText(text);
        applyRunStyle(run, style);
        return true;
    }

    private static RunStyle captureFirstRunStyle(XSLFTextShape shape) {
        for (XSLFTextParagraph p : shape.getTextParagraphs()) {
            for (XSLFTextRun run : p.getTextRuns()) {
                RunStyle style = RunStyle.from(run);
                if (style != null) {
                    return style;
                }
            }
        }
        return null;
    }

    private static void applyRunStyle(XSLFTextRun run, RunStyle style) {
        if (style == null || run == null) {
            return;
        }
        style.applyTo(run);
    }

    private static final class RunStyle {
        final Double fontSize;
        final Boolean bold;
        final Boolean italic;
        final Color fontColor;
        final String fontFamily;

        private RunStyle(
                Double fontSize, Boolean bold, Boolean italic, Color fontColor, String fontFamily) {
            this.fontSize = fontSize;
            this.bold = bold;
            this.italic = italic;
            this.fontColor = fontColor;
            this.fontFamily = fontFamily;
        }

        static RunStyle from(XSLFTextRun run) {
            if (run == null) {
                return null;
            }
            Double size = run.getFontSize();
            if (size != null && size <= 0) {
                size = null;
            }
            return new RunStyle(
                    size,
                    run.isBold(),
                    run.isItalic(),
                    extractSolidColor(run.getFontColor()),
                    run.getFontFamily());
        }

        void applyTo(XSLFTextRun run) {
            if (fontSize != null) {
                run.setFontSize(fontSize);
            }
            if (bold != null) {
                run.setBold(bold);
            }
            if (italic != null) {
                run.setItalic(italic);
            }
            if (fontColor != null) {
                run.setFontColor(fontColor);
            }
            if (fontFamily != null && !fontFamily.isBlank()) {
                run.setFontFamily(fontFamily);
            }
        }
    }

    private static Color extractSolidColor(PaintStyle paint) {
        if (!(paint instanceof PaintStyle.SolidPaint)) {
            return null;
        }
        ColorStyle colorStyle = ((PaintStyle.SolidPaint) paint).getSolidColor();
        return colorStyle != null ? colorStyle.getColor() : null;
    }
}
