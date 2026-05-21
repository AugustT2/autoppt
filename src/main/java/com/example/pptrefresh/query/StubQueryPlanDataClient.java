package com.example.pptrefresh.query;

import org.springframework.stereotype.Component;

import com.example.pptrefresh.time.TradingDayCalendar;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 占位：按条件生成可区分的演示数据（同一 fundCode + 区间应稳定）。
 * TODO: 对接真实按区间批量查询接口。
 */
@Component
public class StubQueryPlanDataClient implements QueryPlanDataClient {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public double fetchAssetClassAllocationPct(String fundCode, String quarter, String assetClass) {
        Random r = new Random(seed(fundCode, quarter, assetClass));
        return 8 + r.nextDouble(25);
    }

    @Override
    public double fetchCumulativeReturnPct(String fundCode, String month, boolean benchmark) {
        if (benchmark) {
            return fetchBenchmarkNavReturnPct(fundCode, "contract_benchmark", month);
        }
        return fetchFundNavReturnPct(fundCode, month);
    }

    @Override
    public double fetchFundNavReturnPct(String fundCode, String timePoint) {
        Random r = new Random(seed(fundCode, "fund", timePoint));
        return round1(r.nextDouble(12) + r.nextDouble(3));
    }

    @Override
    public double fetchBenchmarkNavReturnPct(
            String fundCode, String benchmarkKey, String timePoint) {
        Random r = new Random(seed(fundCode, "bench:" + benchmarkKey, timePoint));
        return round1(r.nextDouble(8) + r.nextDouble(3));
    }

    @Override
    public NavSeriesPoints fetchFundNavReturnsInRange(String fundCode, NavChartTimeRange range) {
        return NavSeriesPoints.of(generateRangeMap(fundCode, "fund", null, range));
    }

    @Override
    public NavSeriesPoints fetchBenchmarkNavReturnsInRange(
            String fundCode, String benchmarkKey, NavChartTimeRange range) {
        return NavSeriesPoints.of(
                generateRangeMap(fundCode, "bench:" + benchmarkKey, benchmarkKey, range));
    }

    private Map<String, Double> generateRangeMap(
            String fundCode, String seriesSeed, String benchmarkKey, NavChartTimeRange range) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (range.granularity() == NavChartAxisGranularity.DAY) {
            List<String> days =
                    range.axisLabels().isEmpty()
                            ? TradingDayCalendar.labelsBetween(
                                    range.startDate(), range.endDate())
                            : range.axisLabels();
            for (String key : days) {
                map.put(
                        key,
                        benchmarkKey == null
                                ? fetchFundNavReturnPct(fundCode, key)
                                : fetchBenchmarkNavReturnPct(fundCode, benchmarkKey, key));
            }
        } else {
            YearMonth start = YearMonth.from(range.startDate());
            YearMonth end = YearMonth.from(range.endDate());
            for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
                String key = ym.format(MONTH);
                map.put(
                        key,
                        benchmarkKey == null
                                ? fetchFundNavReturnPct(fundCode, key)
                                : fetchBenchmarkNavReturnPct(fundCode, benchmarkKey, key));
            }
        }
        return map;
    }

    private static long seed(String fundCode, String a, String b) {
        String key =
                (fundCode == null ? "" : fundCode)
                        + "|"
                        + (a == null ? "" : a)
                        + "|"
                        + (b == null ? "" : b);
        return key.hashCode();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
