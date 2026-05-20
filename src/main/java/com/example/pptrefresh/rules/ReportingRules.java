package com.example.pptrefresh.rules;

import com.example.pptrefresh.query.ManagerTenureRule;

import java.time.LocalDate;

public class ReportingRules {

    private String asOfSource;
    private String asOfDateOverride;
    private String asOfQuarterOverride;
    private String managerTenureRule;
    private String fundInceptionDateOverride;
    private String managerTenureStartOverride;

    public String getAsOfSource() {
        return asOfSource;
    }

    public void setAsOfSource(String asOfSource) {
        this.asOfSource = asOfSource;
    }

    public String getAsOfDateOverride() {
        return asOfDateOverride;
    }

    public LocalDate asOfDateOverrideParsed() {
        return parseDate(asOfDateOverride);
    }

    public String getAsOfQuarterOverride() {
        return asOfQuarterOverride;
    }

    public void setAsOfQuarterOverride(String asOfQuarterOverride) {
        this.asOfQuarterOverride = asOfQuarterOverride;
    }

    public String getManagerTenureRule() {
        return managerTenureRule;
    }

    public void setManagerTenureRule(String managerTenureRule) {
        this.managerTenureRule = managerTenureRule;
    }

    public ManagerTenureRule managerTenureRuleEnum() {
        if (managerTenureRule == null || managerTenureRule.isBlank()) {
            return ManagerTenureRule.EARLIEST;
        }
        return ManagerTenureRule.valueOf(managerTenureRule.trim().toUpperCase());
    }

    public String getFundInceptionDateOverride() {
        return fundInceptionDateOverride;
    }

    public void setFundInceptionDateOverride(String fundInceptionDateOverride) {
        this.fundInceptionDateOverride = fundInceptionDateOverride;
    }

    public LocalDate fundInceptionDateOverrideParsed() {
        return parseDate(fundInceptionDateOverride);
    }

    public String getManagerTenureStartOverride() {
        return managerTenureStartOverride;
    }

    public void setManagerTenureStartOverride(String managerTenureStartOverride) {
        this.managerTenureStartOverride = managerTenureStartOverride;
    }

    public void setAsOfDateOverride(String asOfDateOverride) {
        this.asOfDateOverride = asOfDateOverride;
    }

    public LocalDate managerTenureStartOverrideParsed() {
        return parseDate(managerTenureStartOverride);
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.trim());
    }

}
