package com.example.pptrefresh.document;

/** 图表写回策略：默认仅改 chart XML 缓存，避免 POI 重写嵌入 xlsx 导致 PowerPoint 无法打开。 */
public enum ChartWriteMode {
    /** 只更新 strCache/numCache 与公式，不加载/保存嵌入工作簿（推荐）。 */
    CACHE_ONLY,
    /** 同时更新嵌入工作簿（易触发 POI 重写出损坏的 xlsx，仅调试）。 */
    EMBEDDED_WORKBOOK
}
