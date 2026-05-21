package com.example.pptrefresh.query;

import java.util.regex.Pattern;

/** 识别横轴标签是日频还是月频。 */
public final class ChartTimeLabelParser {

    private static final Pattern DAY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern MONTH = Pattern.compile("^\\d{4}-\\d{2}$");

    private ChartTimeLabelParser() {}

    public static boolean isDayLabel(String label) {
        return label != null && DAY.matcher(label.trim()).matches();
    }

    public static boolean isMonthLabel(String label) {
        return label != null && MONTH.matcher(label.trim()).matches();
    }

    public static QueryCondition timePoint(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("横轴标签为空");
        }
        String trimmed = label.trim();
        if (DAY.matcher(trimmed).matches()) {
            return QueryCondition.dayPoint(trimmed);
        }
        if (MONTH.matcher(trimmed).matches()) {
            return QueryCondition.monthPoint(trimmed);
        }
        return QueryCondition.dayPoint(trimmed);
    }
}
