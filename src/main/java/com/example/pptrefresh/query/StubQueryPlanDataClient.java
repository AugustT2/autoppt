package com.example.pptrefresh.query;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Random;

/**
 * 占位：按条件生成可区分的演示数据（同一 fundCode + 区间应稳定）。
 * TODO: 对接真实按区间/季度/月份查询接口。
 */
@Component
public class StubQueryPlanDataClient implements QueryPlanDataClient {

    @Override
    public double[] fetchAllocationPercents(String fundCode, String quarter) {
        Random r = new Random(seed(fundCode, "alloc", quarter));
        double stock = 50 + r.nextDouble(30);
        double bond = 20 + r.nextDouble(40);
        double cash = Math.max(2, 100 - stock - bond);
        return new double[] {round1(stock), round1(bond), round1(cash)};
    }

    @Override
    public double fetchCumulativeReturnPct(String fundCode, String month, boolean benchmark) {
        Random r = new Random(seed(fundCode, benchmark ? "bench" : "fund", month));
        double base = benchmark ? r.nextDouble(8) : r.nextDouble(12);
        return round1(base + r.nextDouble(3));
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

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
