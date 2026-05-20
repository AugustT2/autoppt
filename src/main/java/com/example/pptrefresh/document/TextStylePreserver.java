package com.example.pptrefresh.document;

import org.apache.poi.sl.usermodel.ColorStyle;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 写回时保留模板已有字体样式。优先就地改 Run/段落，避免 {@code clearText} 或整段 {@code setText}
 * 把同一段内多 Run（如「偏债混-」常规 + 基金名加粗）压成一种样式。
 */
final class TextStylePreserver {

    private TextStylePreserver() {}

    static void setShapeText(XSLFTextShape shape, String text) {
        String value = text == null ? "" : text;
        String existing = shape.getText();
        if (existing != null && existing.equals(value)) {
            return;
        }
        RunStyle style = captureFirstRunStyle(shape);
        if (value.indexOf('\n') < 0
                && value.indexOf('\r') < 0
                && replaceSingleParagraphPreservingRuns(shape, value)) {
            return;
        }
        if (value.indexOf('\n') < 0 && value.indexOf('\r') < 0 && replaceSingleRun(shape, value, style)) {
            return;
        }
        if (replaceMultiParagraph(shape, value)) {
            return;
        }
        shape.clearText();
        XSLFTextRun run = shape.setText(value);
        applyRunStyle(run, style);
    }

    /**
     * 只替换锚点之后的文字；锚点所在段落之前的段落与 Run（含首行「产品线-基金名」混排样式）原样保留。
     */
    private static final Pattern NUMBER_AFTER_LABEL = Pattern.compile("^[：:\\s]*([\\d.,]+)");

    /**
     * 在文本框内定位 {@code fieldLabel}（如「最新规模」），仅替换其后数字，保留标签、单位及其它 Run 样式。
     *
     * @param newNumber 仅数字，如 {@code 58.6}，不含「亿元」
     */
    static void replaceLabeledNumber(XSLFTextShape shape, String fieldLabel, String newNumber) {
        if (fieldLabel == null || fieldLabel.isBlank()) {
            throw new IllegalArgumentException("fieldLabel 不能为空");
        }
        String full = shape.getText();
        if (full == null) {
            full = "";
        }
        int labelIdx = full.indexOf(fieldLabel);
        if (labelIdx < 0) {
            throw new IllegalArgumentException("未找到字段标签: " + fieldLabel);
        }
        int searchFrom = labelIdx + fieldLabel.length();
        Matcher matcher = NUMBER_AFTER_LABEL.matcher(full.substring(searchFrom));
        if (!matcher.find()) {
            throw new IllegalArgumentException("标签后未找到可替换数字: " + fieldLabel);
        }
        int numberStart = searchFrom + matcher.start(1);
        int numberEnd = searchFrom + matcher.end(1);
        String value = newNumber == null ? "" : newNumber.trim();
        replaceCharRange(shape, numberStart, numberEnd, value);
    }

    static void replaceAfterAnchor(XSLFTextShape shape, String anchor, String suffix) {
        String value = suffix == null ? "" : suffix;
        String full = shape.getText();
        if (full == null) {
            full = "";
        }
        int idx = full.indexOf(anchor);
        if (idx < 0) {
            throw new IllegalArgumentException("anchor not found: " + anchor);
        }
        int replaceStart = idx + anchor.length();
        String[] suffixLines = value.split("\\r?\\n", -1);

        List<XSLFTextParagraph> paragraphs = shape.getTextParagraphs();
        if (paragraphs.isEmpty()) {
            setShapeText(shape, full.substring(0, replaceStart) + value);
            return;
        }

        AnchorLocation loc = locateCharOffset(shape, replaceStart);
        if (loc == null) {
            setShapeText(shape, full.substring(0, replaceStart) + value);
            return;
        }

        int lineIdx = 0;
        XSLFTextParagraph anchorParagraph = paragraphs.get(loc.paragraphIndex);
        List<XSLFTextRun> anchorRuns = anchorParagraph.getTextRuns();

        if (!anchorRuns.isEmpty() && loc.runIndex < anchorRuns.size()) {
            XSLFTextRun anchorRun = anchorRuns.get(loc.runIndex);
            String runText = rawText(anchorRun);
            int safeOffset = Math.min(Math.max(loc.offsetInRun, 0), runText.length());
            String prefix = runText.substring(0, safeOffset);
            String firstLine = lineIdx < suffixLines.length ? suffixLines[lineIdx++] : "";
            anchorRun.setText(prefix + firstLine);

            for (int r = loc.runIndex + 1; r < anchorRuns.size(); r++) {
                anchorRuns.get(r).setText("");
            }
        }

        for (int pi = loc.paragraphIndex + 1; pi < paragraphs.size(); pi++) {
            String line = lineIdx < suffixLines.length ? suffixLines[lineIdx++] : "";
            setParagraphPrimaryRunText(paragraphs.get(pi), line);
        }

        if (lineIdx < suffixLines.length) {
            StringBuilder tail = new StringBuilder();
            while (lineIdx < suffixLines.length) {
                if (tail.length() > 0) {
                    tail.append('\n');
                }
                tail.append(suffixLines[lineIdx++]);
            }
            XSLFTextParagraph last = paragraphs.get(paragraphs.size() - 1);
            setParagraphPrimaryRunText(last, rawParagraphText(last) + tail);
        }
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

    /**
     * 单行、单段落、多 Run：按各 Run 原文长度比例切分新文本并保留各 Run 样式（用于 replace_all 仅改字号等场景）。
     * 若新文本与旧文总长度差异过大则放弃。
     */
    private static boolean replaceSingleParagraphPreservingRuns(XSLFTextShape shape, String newText) {
        List<XSLFTextParagraph> paragraphs = shape.getTextParagraphs();
        if (paragraphs.size() != 1) {
            return false;
        }
        List<XSLFTextRun> runs = paragraphs.get(0).getTextRuns();
        if (runs.size() <= 1) {
            return false;
        }

        StringBuilder oldText = new StringBuilder();
        for (XSLFTextRun run : runs) {
            oldText.append(rawText(run));
        }
        if (oldText.length() == 0) {
            return false;
        }

        int[] lengths = new int[runs.size()];
        int total = 0;
        for (int i = 0; i < runs.size(); i++) {
            lengths[i] = rawText(runs.get(i)).length();
            total += lengths[i];
        }
        if (total == 0) {
            return false;
        }

        int pos = 0;
        for (int i = 0; i < runs.size(); i++) {
            XSLFTextRun run = runs.get(i);
            int take;
            if (i == runs.size() - 1) {
                take = newText.length() - pos;
            } else {
                take = (int) Math.round((double) newText.length() * lengths[i] / total);
            }
            if (take < 0) {
                take = 0;
            }
            if (pos + take > newText.length()) {
                take = newText.length() - pos;
            }
            run.setText(newText.substring(pos, pos + take));
            pos += take;
        }
        return pos == newText.length();
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

    private static boolean replaceMultiParagraph(XSLFTextShape shape, String text) {
        String[] lines = text.split("\\r?\\n", -1);
        List<XSLFTextParagraph> paragraphs = shape.getTextParagraphs();
        if (paragraphs.isEmpty() || lines.length != paragraphs.size()) {
            return false;
        }
        for (int i = 0; i < lines.length; i++) {
            if (!setParagraphPrimaryRunText(paragraphs.get(i), lines[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean setParagraphPrimaryRunText(XSLFTextParagraph paragraph, String line) {
        List<XSLFTextRun> runs = paragraph.getTextRuns();
        if (runs.isEmpty()) {
            return false;
        }
        XSLFTextRun run = runs.get(0);
        RunStyle paragraphStyle = RunStyle.from(run);
        run.setText(line == null ? "" : line);
        applyRunStyle(run, paragraphStyle);
        for (int r = 1; r < runs.size(); r++) {
            runs.get(r).setText("");
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

    private static void replaceCharRange(XSLFTextShape shape, int start, int end, String replacement) {
        List<RunSpan> spans = collectRunSpans(shape);
        String repl = replacement == null ? "" : replacement;
        boolean inserted = false;
        for (RunSpan span : spans) {
            if (span.end <= start) {
                continue;
            }
            if (span.start >= end) {
                break;
            }
            String t = span.text;
            int localStart = Math.max(0, start - span.start);
            int localEnd = Math.min(t.length(), end - span.start);
            if (!inserted) {
                String before = t.substring(0, localStart);
                String after = t.substring(localEnd);
                span.run.setText(before + repl + after);
                inserted = true;
            } else {
                String before = t.substring(0, localStart);
                String after = t.substring(localEnd);
                span.run.setText(before + after);
            }
        }
        if (!inserted) {
            throw new IllegalStateException("无法定位替换区间 [" + start + "," + end + ")");
        }
    }

    private static List<RunSpan> collectRunSpans(XSLFTextShape shape) {
        List<RunSpan> spans = new ArrayList<>();
        int pos = 0;
        List<XSLFTextParagraph> paragraphs = shape.getTextParagraphs();
        for (int pi = 0; pi < paragraphs.size(); pi++) {
            if (pi > 0) {
                pos++;
            }
            for (XSLFTextRun run : paragraphs.get(pi).getTextRuns()) {
                String rt = rawText(run);
                spans.add(new RunSpan(run, pos, pos + rt.length(), rt));
                pos += rt.length();
            }
        }
        return spans;
    }

    private static AnchorLocation locateCharOffset(XSLFTextShape shape, int charOffset) {
        int pos = 0;
        List<XSLFTextParagraph> paragraphs = shape.getTextParagraphs();
        for (int pi = 0; pi < paragraphs.size(); pi++) {
            if (pi > 0) {
                pos++;
            }
            List<XSLFTextRun> runs = paragraphs.get(pi).getTextRuns();
            for (int ri = 0; ri < runs.size(); ri++) {
                String rt = rawText(runs.get(ri));
                int len = rt.length();
                if (charOffset <= pos + len) {
                    return new AnchorLocation(pi, ri, charOffset - pos);
                }
                pos += len;
            }
        }
        if (charOffset == pos && !paragraphs.isEmpty()) {
            int lastPi = paragraphs.size() - 1;
            List<XSLFTextRun> runs = paragraphs.get(lastPi).getTextRuns();
            int lastRi = Math.max(0, runs.size() - 1);
            int off = runs.isEmpty() ? 0 : rawText(runs.get(lastRi)).length();
            return new AnchorLocation(lastPi, lastRi, off);
        }
        return null;
    }

    private static String rawText(XSLFTextRun run) {
        String t = run.getRawText();
        return t == null ? "" : t;
    }

    private static String rawParagraphText(XSLFTextParagraph paragraph) {
        StringBuilder sb = new StringBuilder();
        for (XSLFTextRun run : paragraph.getTextRuns()) {
            sb.append(rawText(run));
        }
        return sb.toString();
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

    private static final class RunSpan {
        final XSLFTextRun run;
        final int start;
        final int end;
        final String text;

        RunSpan(XSLFTextRun run, int start, int end, String text) {
            this.run = run;
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }

    private static final class AnchorLocation {
        final int paragraphIndex;
        final int runIndex;
        final int offsetInRun;

        AnchorLocation(int paragraphIndex, int runIndex, int offsetInRun) {
            this.paragraphIndex = paragraphIndex;
            this.runIndex = runIndex;
            this.offsetInRun = offsetInRun;
        }
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
