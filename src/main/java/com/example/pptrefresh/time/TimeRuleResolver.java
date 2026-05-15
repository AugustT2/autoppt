package com.example.pptrefresh.time;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 占位实现：latestDate = 今天；latestQuarter = 上一完整自然季（TBD 可配置化）。
 */
@Component
public class TimeRuleResolver {

    public TimeContext resolve(List<String> timeRules) {
        LocalDate today = LocalDate.now();
        String quarter = latestFullQuarter(today);
        return new TimeContext(today, quarter);
    }

    private static String latestFullQuarter(LocalDate today) {
        int month = today.getMonthValue();
        int year = today.getYear();
        int q = (month - 1) / 3 + 1;
        if (month % 3 != 0 || today.getDayOfMonth() < 28) {
            q -= 1;
            if (q == 0) {
                q = 4;
                year -= 1;
            }
        }
        return year + "Q" + q;
    }
}
