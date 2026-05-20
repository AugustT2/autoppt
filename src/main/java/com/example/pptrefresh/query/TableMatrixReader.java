package com.example.pptrefresh.query;

import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;

import java.util.ArrayList;
import java.util.List;

/** 从 PPT 表格读出完整二维矩阵（空单元格为 {@code ""}）。 */
public final class TableMatrixReader {

    private TableMatrixReader() {}

    public static List<List<String>> read(XSLFTable table) {
        int rows = table.getNumberOfRows();
        int cols = table.getNumberOfColumns();
        List<List<String>> matrix = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            List<String> row = new ArrayList<>(cols);
            for (int c = 0; c < cols; c++) {
                row.add(cellText(table, r, c));
            }
            matrix.add(row);
        }
        return matrix;
    }

    private static String cellText(XSLFTable table, int row, int col) {
        XSLFTableCell cell = table.getCell(row, col);
        if (cell == null) {
            return "";
        }
        String text = cell.getText();
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim();
    }
}
