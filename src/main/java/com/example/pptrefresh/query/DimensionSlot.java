package com.example.pptrefresh.query;

import java.util.List;

public final class DimensionSlot {

    private final int slotIndex;
    private final DimensionSlotRole role;
    private final String label;
    private final QueryCondition condition;
    /** HEADER 槽位：从模板读出的整行表头文案。 */
    private final List<String> columnHeaders;
    /** HEADER 槽位（COLUMN 轴）：第 0 列指标行标签。 */
    private final List<String> rowHeaders;

    public DimensionSlot(int slotIndex, DimensionSlotRole role, String label, QueryCondition condition) {
        this(slotIndex, role, label, condition, null, null);
    }

    public DimensionSlot(
            int slotIndex,
            DimensionSlotRole role,
            String label,
            QueryCondition condition,
            List<String> columnHeaders) {
        this(slotIndex, role, label, condition, columnHeaders, null);
    }

    public DimensionSlot(
            int slotIndex,
            DimensionSlotRole role,
            String label,
            QueryCondition condition,
            List<String> columnHeaders,
            List<String> rowHeaders) {
        this.slotIndex = slotIndex;
        this.role = role;
        this.label = label;
        this.condition = condition;
        this.columnHeaders =
                columnHeaders == null ? null : List.copyOf(columnHeaders);
        this.rowHeaders = rowHeaders == null ? null : List.copyOf(rowHeaders);
    }

    public int slotIndex() {
        return slotIndex;
    }

    public DimensionSlotRole role() {
        return role;
    }

    public String label() {
        return label;
    }

    public QueryCondition condition() {
        return condition;
    }

    public List<String> columnHeaders() {
        return columnHeaders;
    }

    public List<String> rowHeaders() {
        return rowHeaders;
    }
}
