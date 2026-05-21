package com.example.pptrefresh.query;

import com.example.pptrefresh.time.TradingDayCalendar;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 根据模板横轴标签与数据截止日解析折线图取数时间窗。
 *
 * <p>日频图：嵌入表/strCache 为全量交易日，横轴刻度可能经 PPT 自动抽样仅显示少量标签；
 * 取数与写回对齐以 {@link TradingDayCalendar} 在起止日间的全量交易日为准，不以展示抽样点为维度。
 *
 * <p>生产可替换/扩展本类或抽出 {@link #resolve(List, LocalDate)} 对接业务规则（如近一年、成立日）。
 */
@Component
public class NavChartTimeRangeResolver {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * @param templateAxisLabels 模板 strCache/嵌入表横轴分类（日频时为全量交易日；展示抽样不影响本列表点数）
     * @param asOfDate 报告截止日，用于钳制 endDate
     */
    public NavChartTimeRange resolve(List<String> templateAxisLabels, LocalDate asOfDate) {
        if (templateAxisLabels == null || templateAxisLabels.isEmpty()) {
            throw new IllegalArgumentException("模板横轴标签为空");
        }
        List<String> labels =
                templateAxisLabels.stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toList());
        if (labels.isEmpty()) {
            throw new IllegalArgumentException("模板横轴标签为空");
        }
        NavChartAxisGranularity granularity = detectGranularity(labels);
        List<LocalDate> parsed = new ArrayList<>();
        for (String label : labels) {
            parsed.add(parseLabelToDate(label, granularity));
        }
        //开始日期可能跟义务逻辑有关，这里面不管怎么取的，暂时不管，直接复用，省了找逻辑了
        LocalDate start = parsed.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate endRaw = parsed.stream().max(Comparator.naturalOrder()).orElseThrow();
        LocalDate end = asOfDate != null && endRaw.isAfter(asOfDate) ? asOfDate : endRaw;
        if (start.isAfter(end)) {
            start = end;
        }
        List<String> axisLabels = resolveAxisLabels(granularity, labels, start, end);
        return new NavChartTimeRange(start, end, granularity, axisLabels);
    }

    private static List<String> resolveAxisLabels(
            NavChartAxisGranularity granularity,
            List<String> templateLabels,
            LocalDate start,
            LocalDate end) {
        if (granularity != NavChartAxisGranularity.DAY) {
            return templateLabels;
        }
        return TradingDayCalendar.labelsBetween(start, end);
    }

    private static NavChartAxisGranularity detectGranularity(List<String> labels) {
        boolean anyDay =
                labels.stream()
                        .anyMatch(l -> ChartTimeLabelParser.isDayLabel(l));
        if (anyDay) {
            return NavChartAxisGranularity.DAY;
        }
        return NavChartAxisGranularity.MONTH;
    }

    private static LocalDate parseLabelToDate(String label, NavChartAxisGranularity granularity) {
        try {
            if (granularity == NavChartAxisGranularity.DAY) {
                return LocalDate.parse(label, DAY);
            }
            return YearMonth.parse(label, MONTH).atEndOfMonth();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("无法解析横轴日期标签: " + label, e);
        }
    }
}
