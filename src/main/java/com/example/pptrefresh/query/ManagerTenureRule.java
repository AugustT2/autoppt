package com.example.pptrefresh.query;

/** 「任职以来」起点：默认合管取现任中较早任职日；有主基金经理时用 LEAD（见 ReportingRules）。 */
public enum ManagerTenureRule {
    EARLIEST,
    LATEST,
    LEAD
}
