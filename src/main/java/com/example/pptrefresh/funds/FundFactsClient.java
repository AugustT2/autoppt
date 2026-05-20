package com.example.pptrefresh.funds;

import com.example.pptrefresh.query.ManagerTenureRule;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 按基金代码查询基金事实日期（成立日、任职起点等）。生产环境对接真实接口。
 */
public interface FundFactsClient {

    /** 基金成立日期。 */
    Optional<LocalDate> fetchInceptionDate(String fundCode);

    /**
     * 现任基金经理任职起点（合管口径由 {@code tenureRule} 决定取最早/最晚等）。
     */
    Optional<LocalDate> fetchManagerTenureStart(String fundCode, ManagerTenureRule tenureRule);

    /**
     * 最新基金规模数值（仅数字部分，如 {@code 58.6}；单位由模板保留）。
     */
    Optional<String> fetchLatestScale(String fundCode);
}
