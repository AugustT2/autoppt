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

/**
 * 图表数据写回。
 *
 * <p><b>为何 PowerPoint 常提示修复/打不开</b>：{@code chart.getWorkbook()} + {@code saveWorkbook} 会让 POI
 * 重写 {@code ppt/embeddings/*.xlsx}；再经 {@code XMLSlideShow#write} 整包落盘，嵌入表结构易与 chart
 * 外部引用不一致。默认 {@link ChartWriteMode#CACHE_ONLY} 只改 chart 部件里的缓存与公式，嵌入 xlsx 保持模板原样。
 *
 * <p>勿使用 {@code XDDFChart#plot}/{@code setWorkbook(null)}，会拆掉图表关系。
 */
public final class ChartDataWriter {

    private ChartDataWriter() {}

    public static void write(
            XSLFChart chart,
            List<String> categories,
            List<String> seriesNames,
            List<List<Double>> seriesValues,
            ChartWriteMode mode)
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
        rebuildRefCaches(chart, cats, names, values);
        if (mode == ChartWriteMode.EMBEDDED_WORKBOOK) {
            writeEmbeddedWorkbook(chart, cats, names, values);
        } else {
            ChartEmbeddedXlsxPatcher.patch(chart, cats, names, values);
        }
    }

    public static void write(
            XSLFChart chart,
            List<String> categories,
            List<String> seriesNames,
            List<List<Double>> seriesValues)
            throws IOException, InvalidFormatException {
        write(chart, categories, seriesNames, seriesValues, ChartWriteMode.CACHE_ONLY);
    }

    private static void writeEmbeddedWorkbook(
            XSLFChart chart, String[] categories, String[] seriesNames, double[][] seriesValues)
            throws IOException, InvalidFormatException {
        int numSeries = seriesNames.length;
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
        clearRowsBelow(sh, categories.length);
        chart.saveWorkbook(wb);
    }

    private static void clearRowsBelow(XSSFSheet sh, int categoryCount) {
        int lastDataRow0 = categoryCount;
        for (int r = sh.getLastRowNum(); r > lastDataRow0; r--) {
            Row row = sh.getRow(r);
            if (row != null) {
                sh.removeRow(row);
            }
        }
    }

    private static void rebuildRefCaches(
            XSLFChart chart, String[] categories, String[] seriesNames, double[][] seriesValues) {
        if (chart.getCTChart() == null || chart.getCTChart().getPlotArea() == null) {
            return;
        }
        CTPlotArea pa = chart.getCTChart().getPlotArea();
        int target = seriesNames.length;
        for (CTBarChart bc : pa.getBarChartList()) {
            ensureBarSeriesCount(bc, target);
            for (int s = 0; s < target; s++) {
                CTBarSer ser = bc.getSerArray(s);
                refreshSeriesCaches(
                        ser.getTx(),
                        ser.getCat(),
                        ser.getVal(),
                        categories,
                        seriesNames[s],
                        seriesValues[s],
                        s + 1);
            }
        }
        for (CTLineChart lc : pa.getLineChartList()) {
            ensureLineSeriesCount(lc, target);
            for (int s = 0; s < target; s++) {
                CTLineSer ser = lc.getSerArray(s);
                refreshSeriesCaches(
                        ser.getTx(),
                        ser.getCat(),
                        ser.getVal(),
                        categories,
                        seriesNames[s],
                        seriesValues[s],
                        s + 1);
            }
        }
    }

    private static void ensureBarSeriesCount(CTBarChart bc, int target) {
        while (bc.sizeOfSerArray() < target) {
            CTBarSer proto = bc.sizeOfSerArray() > 0 ? bc.getSerArray(bc.sizeOfSerArray() - 1) : null;
            CTBarSer ser = bc.addNewSer();
            int idx = bc.sizeOfSerArray() - 1;
            ser.addNewIdx().setVal(idx);
            ser.addNewOrder().setVal(idx);
            if (proto != null) {
                if (proto.isSetTx()) {
                    ser.addNewTx().set(proto.getTx());
                }
                if (proto.isSetCat()) {
                    ser.addNewCat().set(proto.getCat());
                }
                if (proto.isSetVal()) {
                    ser.addNewVal().set(proto.getVal());
                }
            } else {
                ser.addNewTx();
                ser.addNewCat();
                ser.addNewVal();
            }
        }
        while (bc.sizeOfSerArray() > target) {
            bc.removeSer(bc.sizeOfSerArray() - 1);
        }
        for (int s = 0; s < bc.sizeOfSerArray(); s++) {
            CTBarSer ser = bc.getSerArray(s);
            if (ser.getIdx() == null) {
                ser.addNewIdx().setVal(s);
            }
            if (ser.getOrder() == null) {
                ser.addNewOrder().setVal(s);
            }
        }
    }

    private static void ensureLineSeriesCount(CTLineChart lc, int target) {
        while (lc.sizeOfSerArray() < target) {
            CTLineSer proto = lc.sizeOfSerArray() > 0 ? lc.getSerArray(lc.sizeOfSerArray() - 1) : null;
            CTLineSer ser = lc.addNewSer();
            int idx = lc.sizeOfSerArray() - 1;
            ser.addNewIdx().setVal(idx);
            ser.addNewOrder().setVal(idx);
            if (proto != null) {
                if (proto.isSetTx()) {
                    ser.addNewTx().set(proto.getTx());
                }
                if (proto.isSetCat()) {
                    ser.addNewCat().set(proto.getCat());
                }
                if (proto.isSetVal()) {
                    ser.addNewVal().set(proto.getVal());
                }
            } else {
                ser.addNewTx();
                ser.addNewCat();
                ser.addNewVal();
            }
        }
        while (lc.sizeOfSerArray() > target) {
            lc.removeSer(lc.sizeOfSerArray() - 1);
        }
        for (int s = 0; s < lc.sizeOfSerArray(); s++) {
            CTLineSer ser = lc.getSerArray(s);
            if (ser.getIdx() == null) {
                ser.addNewIdx().setVal(s);
            }
            if (ser.getOrder() == null) {
                ser.addNewOrder().setVal(s);
            }
        }
    }

    private static void refreshSeriesCaches(
            CTSerTx tx,
            CTAxDataSource cat,
            CTNumDataSource val,
            String[] categories,
            String seriesName,
            double[] seriesPoints,
            int seriesCol) {
        int rowEnd = 1 + categories.length;
        if (tx != null && tx.isSetStrRef()) {
            CTStrRef ref = tx.getStrRef();
            ref.setF(sheetRange(seriesCol, 1, 1));
            refillStrCache(ref, new String[] {seriesName});
        }
        if (cat != null && cat.isSetStrRef()) {
            CTStrRef ref = cat.getStrRef();
            ref.setF(sheetRange(0, 2, rowEnd));
            refillStrCache(ref, categories);
        }
        if (val != null && val.isSetNumRef()) {
            CTNumRef ref = val.getNumRef();
            ref.setF(sheetRange(seriesCol, 2, rowEnd));
            refillNumCacheFromDoubles(ref, seriesPoints);
        }
    }

    private static String sheetRange(int col0, int rowStart1, int rowEnd1) {
        return "Sheet1!"
                + cellRef(col0, rowStart1)
                + (rowStart1 == rowEnd1 ? "" : ":" + cellRef(col0, rowEnd1));
    }

    private static String cellRef(int col0, int row1) {
        return "$" + columnLetter(col0) + "$" + row1;
    }

    private static String columnLetter(int col0) {
        if (col0 < 0 || col0 > 25) {
            throw new IllegalArgumentException("列索引超出 A-Z: " + col0);
        }
        return String.valueOf((char) ('A' + col0));
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
