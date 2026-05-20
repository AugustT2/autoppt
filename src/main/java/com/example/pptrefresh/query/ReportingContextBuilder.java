package com.example.pptrefresh.query;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.funds.FundFactsClient;
import com.example.pptrefresh.rules.ReportingRules;
import com.example.pptrefresh.time.TimeContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReportingContextBuilder {

    private static final LocalDate FALLBACK_INCEPTION = LocalDate.of(2019, 6, 12);
    private static final LocalDate FALLBACK_MANAGER_TENURE = LocalDate.of(2020, 1, 15);

    private final FundFactsClient fundFactsClient;

    public ReportingContextBuilder(FundFactsClient fundFactsClient) {
        this.fundFactsClient = fundFactsClient;
    }

    public ReportingContext build(ReportingRules rules, TimeContext time, String fundCode) {
        try {
            LocalDate asOfDate =
                    rules != null && rules.asOfDateOverrideParsed() != null
                            ? rules.asOfDateOverrideParsed()
                            : time.latestDate();
            String asOfQuarter =
                    rules != null && rules.getAsOfQuarterOverride() != null
                            ? rules.getAsOfQuarterOverride()
                            : time.latestQuarter();
            ManagerTenureRule tenureRule =
                    rules != null ? rules.managerTenureRuleEnum() : ManagerTenureRule.EARLIEST;
            LocalDate inception =
                    rules != null && rules.fundInceptionDateOverrideParsed() != null
                            ? rules.fundInceptionDateOverrideParsed()
                            : fundFactsClient
                                    .fetchInceptionDate(fundCode)
                                    .orElse(FALLBACK_INCEPTION);
            LocalDate managerStart =
                    rules != null && rules.managerTenureStartOverrideParsed() != null
                            ? rules.managerTenureStartOverrideParsed()
                            : fundFactsClient
                                    .fetchManagerTenureStart(fundCode, tenureRule)
                                    .orElse(FALLBACK_MANAGER_TENURE);
            return new ReportingContext(asOfDate, asOfQuarter, inception, managerStart);
        } catch (Exception e) {
            throw new RefreshException(
                    FailureStage.REPORTING_CONTEXT,
                    "REPORTING_CONTEXT_FAILED",
                    "无法构建 ReportingContext: " + e.getMessage(),
                    null,
                    e);
        }
    }
}
