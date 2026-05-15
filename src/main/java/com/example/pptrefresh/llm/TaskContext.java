package com.example.pptrefresh.llm;

import com.example.pptrefresh.document.SlideStructure;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.time.TimeContext;

public final class TaskContext {

    private final String deckType;
    /** 从 PPT + 规则解析的基金/产品展示名。 */
    private final String productDisplayName;
    /** 由展示名解析得到的基金代码（演示为硬编码表）。 */
    private final String fundCode;
    private final TimeContext timeContext;
    private final TaskDefinition task;
    private final SlideStructure structure;

    public TaskContext(
            String deckType,
            String productDisplayName,
            String fundCode,
            TimeContext timeContext,
            TaskDefinition task,
            SlideStructure structure) {
        this.deckType = deckType;
        this.productDisplayName = productDisplayName != null ? productDisplayName : "";
        this.fundCode = fundCode != null ? fundCode : "";
        this.timeContext = timeContext;
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

    public TaskDefinition task() {
        return task;
    }

    public SlideStructure structure() {
        return structure;
    }
}
