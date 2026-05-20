package com.example.pptrefresh.query;

import com.example.pptrefresh.time.TimeContext;

import java.time.LocalDate;

/** 报告锚点与基金事实日期；兼容现有 {@link TimeContext} 的 latestDate/latestQuarter。 */
public final class ReportingContext {

    private final LocalDate asOfDate;
    private final String asOfQuarter;
    private final LocalDate fundInceptionDate;
    private final LocalDate managerTenureStartDate;

    public ReportingContext(
            LocalDate asOfDate,
            String asOfQuarter,
            LocalDate fundInceptionDate,
            LocalDate managerTenureStartDate) {
        this.asOfDate = asOfDate;
        this.asOfQuarter = asOfQuarter;
        this.fundInceptionDate = fundInceptionDate;
        this.managerTenureStartDate = managerTenureStartDate;
    }

    public LocalDate asOfDate() {
        return asOfDate;
    }

    public String asOfQuarter() {
        return asOfQuarter;
    }

    public LocalDate fundInceptionDate() {
        return fundInceptionDate;
    }

    public LocalDate managerTenureStartDate() {
        return managerTenureStartDate;
    }

    public TimeContext toTimeContext() {
        return new TimeContext(asOfDate, asOfQuarter);
    }
}
