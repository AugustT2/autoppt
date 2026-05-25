package com.example.pptrefresh.query.metric;

import com.example.pptrefresh.query.QueryCondition;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Random;

/** Stub 指标取数共用的稳定伪随机与格式化。 */
final class StubMetricValues {

    private StubMetricValues() {}

    static long seed(String fundCode, String metricId, QueryCondition condition) {
        String key =
                (fundCode == null ? "" : fundCode)
                        + "|"
                        + (metricId == null ? "" : metricId)
                        + "|"
                        + dateKey(condition);
        return key.hashCode();
    }

    static String formatReturnPct(Random r) {
        double ret = 5 + r.nextDouble(40);
        return String.format("+%.1f%%", ret);
    }

    static String formatPeerRank(Random r) {
        int rank = 1 + r.nextInt(900);
        int total = 850 + r.nextInt(50);
        return rank + " / " + total;
    }

    static String formatPercentile(Random r) {
        int pct = 10 + r.nextInt(80);
        return "前 " + pct + "%";
    }

    static String formatDrawdown(Random r) {
        double dd = 1 + r.nextDouble(15);
        return String.format("-%.1f%%", dd);
    }

    static String formatSharpe(Random r) {
        double s = 0.8 + r.nextDouble() * 1.2;
        return String.format("%.2f", s);
    }

    private static String dateKey(QueryCondition c) {
        if (c == null) {
            return "";
        }
        if (StringUtils.hasText(c.quarter())) {
            return c.quarter();
        }
        if (StringUtils.hasText(c.month())) {
            return c.month();
        }
        LocalDate s = c.startDate();
        LocalDate e = c.endDate();
        if (s != null && e != null) {
            return s + ".." + e;
        }
        return c.label() == null ? "" : c.label();
    }
}
