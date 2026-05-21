package com.example.pptrefresh.document;

import org.apache.poi.xslf.usermodel.XSLFChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrVal;

import java.util.ArrayList;
import java.util.List;

/** 从图表 strCache 读取系列名（图例/嵌入表首行）。 */
public final class ChartSeriesReader {

    private ChartSeriesReader() {}

    public static List<String> readSeriesNames(XSLFChart chart) {
        if (chart == null || chart.getCTChart() == null || chart.getCTChart().getPlotArea() == null) {
            return List.of();
        }
        CTPlotArea pa = chart.getCTChart().getPlotArea();
        for (CTLineChart lc : pa.getLineChartList()) {
            List<String> names = fromLineChart(lc);
            if (!names.isEmpty()) {
                return names;
            }
        }
        for (CTBarChart bc : pa.getBarChartList()) {
            List<String> names = fromBarChart(bc);
            if (!names.isEmpty()) {
                return names;
            }
        }
        return List.of();
    }

    private static List<String> fromLineChart(CTLineChart lc) {
        List<String> out = new ArrayList<>();
        for (CTLineSer ser : lc.getSerList()) {
            String name = fromTx(ser.getTx());
            if (name != null && !name.isBlank()) {
                out.add(name.trim());
            }
        }
        return out;
    }

    private static List<String> fromBarChart(CTBarChart bc) {
        List<String> out = new ArrayList<>();
        for (CTBarSer ser : bc.getSerList()) {
            String name = fromTx(ser.getTx());
            if (name != null && !name.isBlank()) {
                out.add(name.trim());
            }
        }
        return out;
    }

    private static String fromTx(CTSerTx tx) {
        if (tx == null || !tx.isSetStrRef()) {
            return null;
        }
        CTStrRef ref = tx.getStrRef();
        if (ref.isSetStrCache() && ref.getStrCache().sizeOfPtArray() > 0) {
            CTStrVal pt = ref.getStrCache().getPtArray(0);
            return pt != null ? pt.getV() : null;
        }
        return null;
    }
}
