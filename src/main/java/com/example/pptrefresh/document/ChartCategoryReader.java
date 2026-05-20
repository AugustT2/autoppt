package com.example.pptrefresh.document;

import org.apache.poi.xslf.usermodel.XSLFChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrData;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrVal;

import java.util.ArrayList;
import java.util.List;

/** 从图表 strCache 读取横轴分类标签（用于折线/柱图 month/quarter 维度提取）。 */
public final class ChartCategoryReader {

    private ChartCategoryReader() {}

    public static List<String> readCategories(XSLFChart chart) {
        if (chart == null || chart.getCTChart() == null || chart.getCTChart().getPlotArea() == null) {
            return List.of();
        }
        CTPlotArea pa = chart.getCTChart().getPlotArea();
        for (CTBarChart bc : pa.getBarChartList()) {
            if (!bc.getSerList().isEmpty()) {
                return fromCat(bc.getSerList().get(0).getCat());
            }
        }
        for (CTLineChart lc : pa.getLineChartList()) {
            if (!lc.getSerList().isEmpty()) {
                return fromCat(lc.getSerList().get(0).getCat());
            }
        }
        return List.of();
    }

    private static List<String> fromCat(CTAxDataSource cat) {
        if (cat == null || !cat.isSetStrRef()) {
            return List.of();
        }
        CTStrRef ref = cat.getStrRef();
        if (ref.isSetStrCache() && ref.getStrCache().getPtList() != null) {
            List<String> out = new ArrayList<>();
            for (CTStrVal pt : ref.getStrCache().getPtList()) {
                if (pt.getV() != null) {
                    out.add(pt.getV());
                }
            }
            return out;
        }
        if (ref.isSetStrCache()) {
            CTStrData cache = ref.getStrCache();
            if (cache.getPtList() != null) {
                List<String> out = new ArrayList<>();
                for (CTStrVal pt : cache.getPtList()) {
                    if (pt.getV() != null) {
                        out.add(pt.getV());
                    }
                }
                return out;
            }
        }
        return List.of();
    }
}
