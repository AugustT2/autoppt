package com.example.pptrefresh.query;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IntervalConditionResolver {

    private static final Pattern CALENDAR_YEAR = Pattern.compile("^CALENDAR_YEAR_(\\d{4})$");

    public QueryCondition resolve(String strategyKind, String label, ReportingContext ctx) {
        LocalDate end = ctx.asOfDate();
        if (strategyKind == null || strategyKind.isBlank()) {
            return QueryCondition.dateRange(label, end.minusYears(1), end);
        }
        switch (strategyKind) {
            case "ROLLING_7D":
                return QueryCondition.dateRange(label, end.minusDays(7), end);
            case "ROLLING_1M":
                return QueryCondition.dateRange(label, end.minusMonths(1), end);
            case "ROLLING_3M":
                return QueryCondition.dateRange(label, end.minusMonths(3), end);
            case "ROLLING_6M":
                return QueryCondition.dateRange(label, end.minusMonths(6), end);
            case "ROLLING_1Y":
                return QueryCondition.dateRange(label, end.minusYears(1), end);
            case "ROLLING_2Y":
                return QueryCondition.dateRange(label, end.minusYears(2), end);
            case "ROLLING_3Y":
                return QueryCondition.dateRange(label, end.minusYears(3), end);
            case "ROLLING_5Y":
                return QueryCondition.dateRange(label, end.minusYears(5), end);
            case "YTD":
                return QueryCondition.dateRange(label, LocalDate.of(end.getYear(), 1, 1), end);
            case "SINCE_INCEPTION":
                return QueryCondition.sinceInception(
                        label, requireDate(ctx.fundInceptionDate(), "fundInceptionDate"), end);
            case "SINCE_MANAGER_TENURE":
                return QueryCondition.sinceManagerTenure(
                        label, requireDate(ctx.managerTenureStartDate(), "managerTenureStartDate"), end);
            default:
                Matcher m = CALENDAR_YEAR.matcher(strategyKind);
                if (m.matches()) {
                    int year = Integer.parseInt(m.group(1));
                    LocalDate start = LocalDate.of(year, 1, 1);
                    LocalDate yearEnd = LocalDate.of(year, 12, 31);
                    LocalDate effectiveEnd = yearEnd.isAfter(end) ? end : yearEnd;
                    return QueryCondition.dateRange(label, start, effectiveEnd);
                }
                return QueryCondition.dateRange(label, end.minusYears(1), end);
        }
    }

    private static LocalDate requireDate(LocalDate date, String field) {
        if (date == null) {
            throw new IllegalStateException("缺少 " + field + "，无法解析区间条件");
        }
        return date;
    }
}
