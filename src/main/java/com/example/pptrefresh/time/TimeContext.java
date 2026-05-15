package com.example.pptrefresh.time;

import java.time.LocalDate;

public final class TimeContext {

    private final LocalDate latestDate;
    private final String latestQuarter;

    public TimeContext(LocalDate latestDate, String latestQuarter) {
        this.latestDate = latestDate;
        this.latestQuarter = latestQuarter;
    }

    public LocalDate latestDate() {
        return latestDate;
    }

    public String latestQuarter() {
        return latestQuarter;
    }
}
