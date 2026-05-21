package com.example.pptrefresh.query;

import java.time.LocalDate;

public final class QueryCondition {

    private final QueryConditionKind kind;
    private final String label;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String quarter;
    private final String month;
    private final String day;

    private QueryCondition(
            QueryConditionKind kind,
            String label,
            LocalDate startDate,
            LocalDate endDate,
            String quarter,
            String month,
            String day) {
        this.kind = kind;
        this.label = label;
        this.startDate = startDate;
        this.endDate = endDate;
        this.quarter = quarter;
        this.month = month;
        this.day = day;
    }

    public static QueryCondition dateRange(String label, LocalDate start, LocalDate end) {
        return new QueryCondition(QueryConditionKind.DATE_RANGE, label, start, end, null, null, null);
    }

    public static QueryCondition sinceInception(String label, LocalDate start, LocalDate end) {
        return new QueryCondition(
                QueryConditionKind.SINCE_INCEPTION, label, start, end, null, null, null);
    }

    public static QueryCondition sinceManagerTenure(String label, LocalDate start, LocalDate end) {
        return new QueryCondition(
                QueryConditionKind.SINCE_MANAGER_TENURE, label, start, end, null, null, null);
    }

    public static QueryCondition quarterPoint(String quarter) {
        return new QueryCondition(
                QueryConditionKind.QUARTER_POINT, quarter, null, null, quarter, null, null);
    }

    public static QueryCondition monthPoint(String month) {
        return new QueryCondition(
                QueryConditionKind.MONTH_POINT, month, null, null, null, month, null);
    }

    public static QueryCondition dayPoint(String day) {
        return new QueryCondition(QueryConditionKind.DAY_POINT, day, null, null, null, null, day);
    }

    public QueryConditionKind kind() {
        return kind;
    }

    public String label() {
        return label;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public String quarter() {
        return quarter;
    }

    public String month() {
        return month;
    }

    public String day() {
        return day;
    }
}
