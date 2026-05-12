package com.example.autoppt;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumData;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumVal;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrData;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrVal;

import java.io.IOException;

/**
 * 通过嵌入 Excel 写数 + 重建图表 XML 中与 {@code strRef}/{@code numRef} 配套的缓存，避免 PowerPoint
 * 校验失败。
 */
public final class ChartSlideFiller {

    public static final String CHART_ALLOCATION = "CHART_ALLOCATION";
    public static final String CHART_NAV_SERIES = "CHART_NAV_SERIES";

    private ChartSlideFiller() {}

    public static void fillCharts(XSLFSlide slide, FundCharts charts) throws IOException, InvalidFormatException {
        writeEmbeddedChartSheet(
                chartOf(slide, CHART_ALLOCATION),
                charts.allocCategories(),
                charts.allocSeriesNames(),
                charts.allocSeriesValues());
        writeEmbeddedChartSheet(
                chartOf(slide, CHART_NAV_SERIES),
                charts.navCategories(),
                charts.navSeriesNames(),
                charts.navSeriesValues());
    }

    private static XSLFChart chartOf(XSLFSlide slide, String shapeName) {
        XSLFShape sh = ShapeFinder.find(slide, shapeName);
        if (sh == null) {
            throw new IllegalStateException("缺少图表框: " + shapeName);
        }
        if (!(sh instanceof XSLFGraphicFrame)) {
            throw new IllegalStateException(shapeName + " 不是 GraphicFrame: " + sh.getClass());
        }
        XSLFGraphicFrame frame = (XSLFGraphicFrame) sh;
        if (!frame.hasChart()) {
            throw new IllegalStateException(shapeName + " 内无图表");
        }
        return frame.getChart();
    }

    /**
     * 布局与模板一致：A 列分类（自第 2 行），B1/C1 为系列名，B/C 列第 2 行起为各系列数值；随后重建
     * {@code strCache}/{@code numCache} 与单元格一致。
     */
    private static void writeEmbeddedChartSheet(
            XSLFChart chart, String[] categories, String[] seriesNames, double[][] seriesValues)
            throws IOException, InvalidFormatException {
        if (categories.length == 0) {
            throw new IllegalArgumentException("分类轴不能为空");
        }
        int numSeries = seriesNames.length;
        if (seriesValues.length != numSeries) {
            throw new IllegalArgumentException("系列名个数与 seriesValues 行数不一致");
        }
        for (int s = 0; s < numSeries; s++) {
            if (seriesValues[s].length != categories.length) {
                throw new IllegalArgumentException(
                        "系列 " + s + " 点数 " + seriesValues[s].length + " 与分类数 " + categories.length + " 不一致");
            }
        }

        XSSFWorkbook wb = chart.getWorkbook();
        XSSFSheet sh = wb.getSheetAt(0);

        Row row0 = getOrCreateRow(sh, 0);
        for (int s = 0; s < numSeries; s++) {
            getOrCreateCell(row0, 1 + s).setCellValue(seriesNames[s]);
        }

        for (int i = 0; i < categories.length; i++) {
            Row row = getOrCreateRow(sh, 1 + i);
            getOrCreateCell(row, 0).setCellValue(categories[i]);
            for (int s = 0; s < numSeries; s++) {
                getOrCreateCell(row, 1 + s).setCellValue(seriesValues[s][i]);
            }
        }

        chart.saveWorkbook(wb);
        /*
         * POI 的 XDDFChart.commit() 在整包写出时会再次 saveWorkbook(this.workbook)。多图场景下
         * 嵌入工作簿实例可能被复用，最后一次写入会覆盖各图表部件对应的 xlsx，导致「图表缓存 /
         * 公式仍指向旧数，嵌入表已是最后一只基金」——PowerPoint 会提示修复并删内容。写盘后丢弃缓存，
         * 让 commit 只写 chartSpace XML，避免二次把共享 workbook 刷进错误的 PackagePart。
         */
        chart.setWorkbook(null);
        rebuildRefCaches(chart, categories, seriesNames, seriesValues);
    }

    private static void rebuildRefCaches(
            XSLFChart chart, String[] categories, String[] seriesNames, double[][] seriesValues) {
        if (chart.getCTChart() == null || chart.getCTChart().getPlotArea() == null) {
            return;
        }
        CTPlotArea pa = chart.getCTChart().getPlotArea();
        for (CTBarChart bc : pa.getBarChartList()) {
            int s = 0;
            for (CTBarSer ser : bc.getSerList()) {
                refreshSeriesCaches(ser.getTx(), ser.getCat(), ser.getVal(), categories, seriesNames[s], seriesValues[s]);
                s++;
            }
        }
        for (CTLineChart lc : pa.getLineChartList()) {
            int s = 0;
            for (CTLineSer ser : lc.getSerList()) {
                refreshSeriesCaches(ser.getTx(), ser.getCat(), ser.getVal(), categories, seriesNames[s], seriesValues[s]);
                s++;
            }
        }
    }

    private static void refreshSeriesCaches(
            CTSerTx tx,
            CTAxDataSource cat,
            CTNumDataSource val,
            String[] categories,
            String seriesName,
            double[] seriesPoints) {
        if (tx != null && tx.isSetStrRef()) {
            refillStrCache(tx.getStrRef(), new String[] {seriesName});
        }
        if (cat != null && cat.isSetStrRef()) {
            refillStrCache(cat.getStrRef(), categories);
        }
        if (val != null && val.isSetNumRef()) {
            refillNumCacheFromDoubles(val.getNumRef(), seriesPoints);
        }
    }

    private static void refillStrCache(CTStrRef ref, String[] values) {
        CTStrData cache = ref.isSetStrCache() ? ref.getStrCache() : ref.addNewStrCache();
        while (cache.sizeOfPtArray() > 0) {
            cache.removePt(cache.sizeOfPtArray() - 1);
        }
        for (int i = 0; i < values.length; i++) {
            CTStrVal pt = cache.addNewPt();
            pt.setIdx(i);
            pt.setV(values[i]);
        }
        if (cache.isSetPtCount()) {
            cache.getPtCount().setVal(values.length);
        } else {
            cache.addNewPtCount().setVal(values.length);
        }
    }

    private static void refillNumCacheFromDoubles(CTNumRef ref, double[] values) {
        CTNumData cache = ref.isSetNumCache() ? ref.getNumCache() : ref.addNewNumCache();
        while (cache.sizeOfPtArray() > 0) {
            cache.removePt(cache.sizeOfPtArray() - 1);
        }
        cache.setFormatCode("General");
        for (int i = 0; i < values.length; i++) {
            CTNumVal pt = cache.addNewPt();
            pt.setIdx(i);
            pt.setV(String.valueOf(values[i]));
        }
        if (cache.isSetPtCount()) {
            cache.getPtCount().setVal(values.length);
        } else {
            cache.addNewPtCount().setVal(values.length);
        }
    }

    private static Row getOrCreateRow(XSSFSheet sh, int rowIndex0) {
        Row row = sh.getRow(rowIndex0);
        if (row == null) {
            row = sh.createRow(rowIndex0);
        }
        return row;
    }

    private static Cell getOrCreateCell(Row row, int colIndex0) {
        Cell cell = row.getCell(colIndex0);
        if (cell == null) {
            cell = row.createCell(colIndex0);
        }
        return cell;
    }
}
