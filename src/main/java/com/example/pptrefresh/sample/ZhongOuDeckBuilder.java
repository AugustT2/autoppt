package com.example.pptrefresh.sample;

import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 根据「偏债混-中欧瑾添」一页式材料生成合规文件名 pptx：{@code 20260430-偏债混-M1.pptx}。
 * 左侧：文案 + 业绩表；右侧：大类资产<strong>柱状图</strong> + 累计收益<strong>折线图</strong>。
 */
public final class ZhongOuDeckBuilder {

    private static final int SLIDE_W = 960;
    private static final int SLIDE_H = 540;

    private ZhongOuDeckBuilder() {}

    public static void main(String[] args) throws Exception {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path out =
                args.length >= 1
                        ? Paths.get(args[0])
                        : projectDir.resolve("samples").resolve("20260430-\u504f\u503a\u6df7-M1.pptx");
        Files.createDirectories(out.getParent());
        build(out);
        System.out.println("Written: " + out.toAbsolutePath());
    }

    public static void build(Path output) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension(SLIDE_W, SLIDE_H));
            XSLFSlideMaster master = ppt.getSlideMasters().get(0);
            XSLFSlideLayout blank = master.getLayout(SlideLayout.BLANK);
            XSLFSlide slide = ppt.createSlide(blank);

            addTitleBlock(slide);
            addFundMetaBlock(slide);
            addStrategyBlock(slide);
            addBrandBlock(slide);
            addPerformanceTable(slide);
            addAllocationBarChart(ppt, slide);
            addNavLineChart(ppt, slide);
            addFooter(slide);

            Path temp = output.resolveSibling(output.getFileName() + ".writing");
            try (OutputStream os = Files.newOutputStream(temp)) {
                ppt.write(os);
            }
            Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 右侧上图：分组柱状图（刷新 chartOrdinal: 1）。 */
    private static void addAllocationBarChart(XMLSlideShow ppt, XSLFSlide slide) throws IOException {
        XSLFTextBox label = slide.createTextBox();
        label.setAnchor(new Rectangle(470, 52, 320, 22));
        setText(label, "大类资产配置情况（%）", 11, true, new Color(0x1a, 0x3d, 0x6b));

        XSLFChart chart = ppt.createChart();
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottom = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis left = chart.createValueAxis(AxisPosition.LEFT);
        left.setTitle("%");

        XDDFBarChartData bar =
                (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottom, left);
        bar.setBarDirection(BarDirection.COL);
        bar.setBarGrouping(org.apache.poi.xddf.usermodel.chart.BarGrouping.CLUSTERED);

        String[] cats = {"股票", "可转债", "利率债", "信用债"};
        XDDFDataSource<String> catSource = XDDFDataSourcesFactory.fromArray(cats);

        String[] quarterNames = {"2025Q2", "2025Q3", "2025Q4", "2026Q1"};
        Double[][] values = {
            {29d, 7d, 23d, 24d},
            {35d, 2d, 20d, 15d},
            {38d, 1d, 20d, 22d},
            {32d, 0d, 12d, 48d}
        };
        for (int q = 0; q < quarterNames.length; q++) {
            XDDFNumericalDataSource<Double> vals =
                    XDDFDataSourcesFactory.fromArray(values[q]);
            XDDFChartData.Series series = bar.addSeries(catSource, vals);
            series.setTitle(quarterNames[q], null);
        }

        chart.plot(bar);
        chart.getOrAddLegend().setPosition(LegendPosition.TOP);

        slide.addChart(chart, new Rectangle2D.Double(460, 76, 485, 205));
    }

    /** 右侧下图：双折线图（刷新 chartOrdinal: 2）。 */
    private static void addNavLineChart(XMLSlideShow ppt, XSLFSlide slide) throws IOException {
        XSLFTextBox label = slide.createTextBox();
        label.setAnchor(new Rectangle(470, 288, 320, 22));
        setText(label, "累计收益率走势（%）", 11, true, new Color(0x1a, 0x3d, 0x6b));

        XSLFChart chart = ppt.createChart();
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottom = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis left = chart.createValueAxis(AxisPosition.LEFT);
        left.setTitle("%");
        left.setCrossBetween(org.apache.poi.xddf.usermodel.chart.AxisCrossBetween.BETWEEN);

        XDDFLineChartData line =
                (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottom, left);

        String[] dates = {
            "2024-01", "2024-07", "2025-01", "2025-07", "2026-01", "2026-04"
        };
        XDDFDataSource<String> catSource = XDDFDataSourcesFactory.fromArray(dates);
        Double[] fund = {0d, -2d, 2d, 8d, 10d, 12.5d};
        Double[] idx = {2d, 4d, 6d, 10d, 14d, 16d};

        XDDFLineChartData.Series s1 =
                (XDDFLineChartData.Series) line.addSeries(catSource, XDDFDataSourcesFactory.fromArray(fund));
        s1.setTitle("中欧瑾添A", null);
        s1.setSmooth(false);
        XDDFLineChartData.Series s2 =
                (XDDFLineChartData.Series) line.addSeries(catSource, XDDFDataSourcesFactory.fromArray(idx));
        s2.setTitle("万得混合债券型二级指数", null);
        s2.setSmooth(false);

        chart.plot(line);
        chart.getOrAddLegend().setPosition(LegendPosition.BOTTOM);

        slide.addChart(chart, new Rectangle2D.Double(460, 312, 485, 200));
    }

    private static void addTitleBlock(XSLFSlide slide) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(24, 16, 420, 36));
        setText(box, "偏债混-中欧瑾添", 20, true, new Color(0x1a, 0x3d, 0x6b));
    }

    private static void addFundMetaBlock(XSLFSlide slide) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(24, 52, 420, 56));
        setText(
                box,
                "A:013998  C:013999  |  成立日：2021-11-09（任职：2024-08-23）\n"
                        + "基金经理：王申、赵煜澄  |  最新规模：2.83亿",
                10,
                false,
                Color.DARK_GRAY);
    }

    private static void addStrategyBlock(XSLFSlide slide) {
        XSLFTextBox label = slide.createTextBox();
        label.setAnchor(new Rectangle(24, 112, 200, 22));
        setText(label, "投资范围及策略", 11, true, new Color(0x1a, 0x3d, 0x6b));

        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(24, 134, 420, 188));
        setText(
                box,
                "以30%权益中枢的高波收益+策略，在控制波动与回撤的前提下增强收益。\n"
                        + "股票：采用smart beta宏观择时与自下而上宏观敏感度体系。\n"
                        + "可转债：采用估值交易指数体系与量化模型获取可持续alpha。\n"
                        + "纯债：高等级信用债为底仓，辅以利率交易。",
                9,
                false,
                Color.BLACK);
    }

    private static void addBrandBlock(XSLFSlide slide) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(720, 16, 220, 48));
        setText(box, "中欧基金\n用长期业绩说话", 12, true, new Color(0xc0, 0x30, 0x30));
        for (XSLFTextParagraph p : box.getTextParagraphs()) {
            p.setTextAlign(TextParagraph.TextAlign.RIGHT);
        }
    }

    /** 第 1 个表：业绩指标（tableOrdinal: 1） */
    private static void addPerformanceTable(XSLFSlide slide) {
        XSLFTextBox label = slide.createTextBox();
        label.setAnchor(new Rectangle(24, 328, 120, 20));
        setText(label, "业绩指标", 11, true, new Color(0x1a, 0x3d, 0x6b));

        String[][] data = {
            {"指标", "2025年", "YTD", "任职以来", "近一年", "近六个月"},
            {"累计收益", "7.87%", "4.63%", "21.08%", "13.39%", "6.20%"},
            {"年化收益", "7.87%", "14.77%", "12%", "13.39%", "12.18%"},
            {"收益排名", "--", "22%", "22%", "22%", "22%"},
            {"二级债基指数(年化)", "5.76%", "7.08%", "7.91%", "7.58%", "4.76%"},
            {"夏普比率", "1.36", "1.84", "1.65", "2.02", "1.60"},
            {"最大回撤", "-2.81%", "-4.43%", "-4.43%", "-4.43%", "-4.43%"}
        };
        placeTable(slide, new Rectangle(24, 350, 420, 118), data);
    }

    private static void addFooter(XSLFSlide slide) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(24, 502, 560, 18));
        setText(box, "数据截点：2026/4/30  |  数据来源：WIND，中欧基金", 8, false, Color.GRAY);
    }

    private static void placeTable(XSLFSlide slide, Rectangle anchor, String[][] data) {
        int rows = data.length;
        int cols = data[0].length;
        XSLFTable table = slide.createTable(rows, cols);
        table.setAnchor(anchor);
        for (int r = 0; r < rows; r++) {
            XSLFTableRow row = table.getRows().get(r);
            row.setHeight(16);
            for (int c = 0; c < cols; c++) {
                XSLFTableCell cell = row.getCells().get(c);
                cell.setText(data[r][c]);
                for (XSLFTextParagraph p : cell.getTextParagraphs()) {
                    for (XSLFTextRun run : p.getTextRuns()) {
                        run.setFontFamily("微软雅黑");
                        run.setFontSize(r == 0 ? 8.0 : 7.5);
                        run.setBold(r == 0);
                    }
                }
            }
        }
    }

    private static void setText(
            XSLFTextBox box, String text, double fontSize, boolean bold, Color color) {
        box.clearText();
        XSLFTextRun run = box.setText(text);
        run.setFontFamily("微软雅黑");
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontColor(color);
        for (XSLFTextParagraph p : box.getTextParagraphs()) {
            p.setTextAlign(TextParagraph.TextAlign.LEFT);
        }
    }
}
