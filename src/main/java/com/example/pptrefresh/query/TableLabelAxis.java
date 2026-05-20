package com.example.pptrefresh.query;

/**
 * 表格区间标签所在轴。
 *
 * <ul>
 *   <li>{@link #ROW} — 一行一个区间，标签在某一列（常见：第 0 列「近一年」「近两年」）
 *   <li>{@link #COLUMN} — 一列一个区间，标签在某一行（常见：第 0 行表头横排区间）
 * </ul>
 */
public enum TableLabelAxis {
    ROW,
    COLUMN;

    public static TableLabelAxis fromYaml(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("labelAxis 不能为空");
        }
        String n = text.trim().toUpperCase();
        if ("ROW".equals(n) || "ROWS".equals(n) || "LABEL_IN_COLUMN".equals(n)) {
            return ROW;
        }
        if ("COLUMN".equals(n) || "COLS".equals(n) || "LABEL_IN_ROW".equals(n)) {
            return COLUMN;
        }
        throw new IllegalArgumentException("未知 labelAxis: " + text);
    }
}
