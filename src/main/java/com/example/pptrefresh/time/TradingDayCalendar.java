package com.example.pptrefresh.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** A 股口径：周一至周五（节假日后续可扩展）。 */
public final class TradingDayCalendar {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private TradingDayCalendar() {}

    /** 闭区间 [start, end] 内每个交易日，标签 yyyy-MM-dd。 */
    public static List<String> labelsBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("无效交易日区间: " + start + " ~ " + end);
        }
        List<String> out = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (isTradingDay(d)) {
                out.add(d.format(DAY));
            }
        }
        return out;
    }

    public static boolean isTradingDay(LocalDate date) {
        DayOfWeek w = date.getDayOfWeek();
        return w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY;
    }
}
