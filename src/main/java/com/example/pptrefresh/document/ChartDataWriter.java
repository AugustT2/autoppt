package com.example.pptrefresh.document;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.XSLFChart;
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
import java.util.List;

/** 嵌入表 + 图表缓存写数（POI 柱状/折线图数据与 strCache/numCache 同步）。 */
public final class ChartDataWriter {

    private ChartDataWriter() {}

    public static void write(
            XSLFChart chart,
            List<String> categories,
            List<String> seriesNames,
            List<List<Double>> seriesValues)
            throws IOException, InvalidFormatException {
        String[] cats = categories.toArray(String[]::new);
        String[] names = seriesNames.toArray(String[]::new);
        double[][] values = new double[seriesValues.size()][];
        for (int i = 0; i < seriesValues.size(); i++) {
            List<Double> row = seriesValues.get(i);
            values[i] = new double[row.size()];
            for (int j = 0; j < row.size(); j++) {
                values[i][j] = row.get(j);
            }
        }
        writeEmbeddedChartSheet(chart, cats, names, values);
    }

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
                        "系列 " + s + " 点数与分类数不一致");
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
                refreshSeriesCaches(
                        ser.getTx(), ser.getCat(), ser.getVal(), categories, seriesNames[s], seriesValues[s]);
                s++;
            }
        }
        for (CTLineChart lc : pa.getLineChartList()) {
            int s = 0;
            for (CTLineSer ser : lc.getSerList()) {
                refreshSeriesCaches(
                        ser.getTx(), ser.getCat(), ser.getVal(), categories, seriesNames[s], seriesValues[s]);
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
