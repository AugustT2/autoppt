package com.example.pptrefresh.query;

import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;

import java.util.ArrayList;
import java.util.List;

public final class TableIntervalDimensionExtractor {

    private TableIntervalDimensionExtractor() {}

    /** 按轴读取区间标签；{@link TableLabelAxis#AUTO} 时需传入词表。 */
    public static TableLabelScanResult scan(
            XSLFTable table,
            TableLabelAxis axis,
            int labelIndex,
            int headerSpan,
            IntervalLexicon lexicon) {
        if (axis == TableLabelAxis.AUTO) {
            if (lexicon == null) {
                throw new IllegalArgumentException("AUTO 模式需要 IntervalLexicon");
            }
            return autoDetect(table, labelIndex, headerSpan, lexicon);
        }
        List<String> labels =
                axis == TableLabelAxis.ROW
                        ? readLabelsInColumn(table, labelIndex, headerSpan)
                        : readLabelsInRow(table, labelIndex, headerSpan);
        List<String> headerLabels = readTableHeader(table, axis, labelIndex, headerSpan);
        List<String> rowHeaderLabels =
                axis == TableLabelAxis.COLUMN
                        ? readLabelsInColumn(table, 0, headerSpan)
                        : List.of();
        return new TableLabelScanResult(
                axis, labelIndex, headerSpan, labels, headerLabels, rowHeaderLabels);
    }

    /**
     * 读取表头单元格文案（不参与区间词表匹配）。
     *
     * <ul>
     *   <li>ROW：表头为数据区上一行（第 {@code headerSpan - 1} 行）整行</li>
     *   <li>COLUMN：表头为区间标签所在行（{@code labelIndex} 行）整行</li>
     * </ul>
     */
    public static List<String> readTableHeader(
            XSLFTable table, TableLabelAxis axis, int labelIndex, int headerSpan) {
        if (axis == TableLabelAxis.ROW) {
            return readHeaderInRow(table, headerSpan);
        }
        if (axis == TableLabelAxis.COLUMN) {
            return readHeaderInRow(table, labelIndex);
        }
        throw new IllegalArgumentException("readTableHeader 需要已确定的 labelAxis，不能为 AUTO");
    }

    /** 兼容旧调用：等价于 {@link TableLabelAxis#ROW} + 指定列。 */
    public static List<String> readIntervalLabels(XSLFTable table, int intervalColumn, int headerRows) {
        return readLabelsInColumn(table, intervalColumn, headerRows);
    }

    static List<String> readLabelsInColumn(XSLFTable table, int columnIndex, int headerRows) {
        List<String> labels = new ArrayList<>();
        int rows = table.getNumberOfRows();
        for (int r = headerRows; r < rows; r++) {
            String text = cellText(table, r, columnIndex);
            if (text != null) {
                labels.add(text);
            }
        }
        return labels;
    }

    /** ROW 轴：跳过 {@code headerSpan} 行后的数据区上方一行作为表头。 */
    static List<String> readHeaderInRow(XSLFTable table, int headerSpan) {
        int headerRow = Math.max(0, headerSpan - 1);
        return readRowCells(table, headerRow);
    }

    static List<String> readRowCells(XSLFTable table, int rowIndex) {
        List<String> cells = new ArrayList<>();
        int cols = table.getNumberOfColumns();
        for (int c = 0; c < cols; c++) {
            String text = cellText(table, rowIndex, c);
            cells.add(text != null ? text : "");
        }
        return cells;
    }

    static List<String> readLabelsInRow(XSLFTable table, int rowIndex, int headerCols) {
        List<String> labels = new ArrayList<>();
        int cols = table.getNumberOfColumns();
        for (int c = headerCols; c < cols; c++) {
            String text = cellText(table, rowIndex, c);
            if (text != null) {
                labels.add(text);
            }
        }
        return labels;
    }

    private static TableLabelScanResult autoDetect(
            XSLFTable table, int labelIndex, int headerSpan, IntervalLexicon lexicon) {
        List<String> inColumn = readLabelsInColumn(table, labelIndex, headerSpan);
        List<String> inRow = readLabelsInRow(table, labelIndex, headerSpan);
        int columnHits = lexiconHitCount(lexicon, inColumn);
        int rowHits = lexiconHitCount(lexicon, inRow);
        if (columnHits >= rowHits && columnHits > 0) {
            TableLabelAxis axis = TableLabelAxis.ROW;
            return scanResult(table, axis, labelIndex, headerSpan, inColumn);
        }
        if (rowHits > 0) {
            TableLabelAxis axis = TableLabelAxis.COLUMN;
            return scanResult(table, axis, labelIndex, headerSpan, inRow);
        }
        if (!inColumn.isEmpty()) {
            TableLabelAxis axis = TableLabelAxis.ROW;
            return scanResult(table, axis, labelIndex, headerSpan, inColumn);
        }
        if (!inRow.isEmpty()) {
            TableLabelAxis axis = TableLabelAxis.COLUMN;
            return scanResult(table, axis, labelIndex, headerSpan, inRow);
        }
        return new TableLabelScanResult(
                TableLabelAxis.AUTO, labelIndex, headerSpan, List.of(), List.of(), List.of());
    }

    private static TableLabelScanResult scanResult(
            XSLFTable table,
            TableLabelAxis axis,
            int labelIndex,
            int headerSpan,
            List<String> labels) {
        List<String> rowHeaderLabels =
                axis == TableLabelAxis.COLUMN
                        ? readLabelsInColumn(table, 0, headerSpan)
                        : List.of();
        return new TableLabelScanResult(
                axis,
                labelIndex,
                headerSpan,
                labels,
                readTableHeader(table, axis, labelIndex, headerSpan),
                rowHeaderLabels);
    }

    private static int lexiconHitCount(IntervalLexicon lexicon, List<String> labels) {
        int hits = 0;
        for (String label : labels) {
            if (lexicon.resolveKind(label) != null) {
                hits++;
            }
        }
        return hits;
    }

    private static String cellText(XSLFTable table, int row, int col) {
        XSLFTableCell cell = table.getCell(row, col);
        if (cell == null) {
            return null;
        }
        String text = cell.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    public static final class TableLabelScanResult {
        private final TableLabelAxis axis;
        private final int labelIndex;
        private final int headerSpan;
        private final List<String> labels;
        private final List<String> headerLabels;
        /** COLUMN 轴时：第 0 列指标名（收益率、同类排名…）。 */
        private final List<String> rowHeaderLabels;

        TableLabelScanResult(
                TableLabelAxis axis,
                int labelIndex,
                int headerSpan,
                List<String> labels,
                List<String> headerLabels,
                List<String> rowHeaderLabels) {
            this.axis = axis;
            this.labelIndex = labelIndex;
            this.headerSpan = headerSpan;
            this.labels = List.copyOf(labels);
            this.headerLabels = List.copyOf(headerLabels);
            this.rowHeaderLabels = List.copyOf(rowHeaderLabels);
        }

        public TableLabelAxis axis() {
            return axis;
        }

        public int labelIndex() {
            return labelIndex;
        }

        public int headerSpan() {
            return headerSpan;
        }

        public List<String> labels() {
            return labels;
        }

        public List<String> headerLabels() {
            return headerLabels;
        }

        public List<String> rowHeaderLabels() {
            return rowHeaderLabels;
        }
    }
}
