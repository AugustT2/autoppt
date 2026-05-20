package com.example.pptrefresh.query;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DateQuarterUtil {

    private static final Pattern QUARTER = Pattern.compile("^(\\d{4})Q([1-4])$");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private DateQuarterUtil() {}

    static String previousQuarter(String quarter) {
        Matcher m = QUARTER.matcher(quarter);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid quarter: " + quarter);
        }
        int year = Integer.parseInt(m.group(1));
        int q = Integer.parseInt(m.group(2));
        if (q == 1) {
            return (year - 1) + "Q4";
        }
        return year + "Q" + (q - 1);
    }

    static List<String> rollingQuartersEndingAt(String endQuarter, int count) {
        List<String> out = new ArrayList<>(count);
        String q = endQuarter;
        for (int i = 0; i < count; i++) {
            out.add(0, q);
            q = previousQuarter(q);
        }
        return out;
    }

    static LocalDate quarterEndDate(String quarter) {
        Matcher m = QUARTER.matcher(quarter);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid quarter: " + quarter);
        }
        int year = Integer.parseInt(m.group(1));
        int q = Integer.parseInt(m.group(2));
        return switch (q) {
            case 1 -> LocalDate.of(year, 3, 31);
            case 2 -> LocalDate.of(year, 6, 30);
            case 3 -> LocalDate.of(year, 9, 30);
            case 4 -> LocalDate.of(year, 12, 31);
            default -> throw new IllegalStateException();
        };
    }

    static List<String> rollingMonthsEndingAt(LocalDate asOfDate, int count) {
        YearMonth end = YearMonth.from(asOfDate);
        List<String> out = new ArrayList<>(count);
        for (int i = count - 1; i >= 0; i--) {
            out.add(end.minusMonths(i).format(MONTH));
        }
        return out;
    }

    static LocalDate monthEndDate(String yyyyMm) {
        YearMonth ym = YearMonth.parse(yyyyMm, MONTH);
        return ym.atEndOfMonth();
    }
}
