package com.example.pptrefresh.llm;

import com.example.pptrefresh.document.SlideStructure;
import com.example.pptrefresh.query.QueryPlan;
import com.example.pptrefresh.query.ReportingContext;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.time.TimeContext;

public final class TaskContext {

    private final String deckType;
    private final String productDisplayName;
    private final String fundCode;
    private final TimeContext timeContext;
    private final ReportingContext reportingContext;
    private final QueryPlan queryPlan;
    private final TaskDefinition task;
    private final SlideStructure structure;

    public TaskContext(
            String deckType,
            String productDisplayName,
            String fundCode,
            TimeContext timeContext,
            ReportingContext reportingContext,
            QueryPlan queryPlan,
            TaskDefinition task,
            SlideStructure structure) {
        this.deckType = deckType;
        this.productDisplayName = productDisplayName != null ? productDisplayName : "";
        this.fundCode = fundCode != null ? fundCode : "";
        this.timeContext = timeContext;
        this.reportingContext = reportingContext;
        this.queryPlan = queryPlan;
        this.task = task;
        this.structure = structure;
    }

    public String deckType() {
        return deckType;
    }

    public String productDisplayName() {
        return productDisplayName;
    }

    public String fundCode() {
        return fundCode;
    }

    public TimeContext timeContext() {
        return timeContext;
    }

    public ReportingContext reportingContext() {
        return reportingContext;
    }

    public QueryPlan queryPlan() {
        return queryPlan;
    }

    public TaskDefinition task() {
        return task;
    }

    public SlideStructure structure() {
        return structure;
    }
}
