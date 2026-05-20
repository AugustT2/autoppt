package com.example.pptrefresh.query;

public enum DimensionSlotRole {
    HEADER,
    /** 区间标签在列上：每个数据行一个区间 */
    DATA_ROW,
    /** 区间标签在行上：每个数据列一个区间 */
    DATA_COLUMN,
    CATEGORY
}
