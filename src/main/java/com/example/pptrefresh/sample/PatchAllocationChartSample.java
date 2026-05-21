package com.example.pptrefresh.sample;

import com.example.pptrefresh.document.ChartDataWriter;
import com.example.pptrefresh.document.ShapeWalker;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 按当前样例 PPT 模板（标题等版式已在模板中）仅刷新右上资产配置图数据。用法：
 * {@code mvn -q exec:java -Dexec.mainClass=com.example.pptrefresh.sample.PatchAllocationChartSample}
 */
public final class PatchAllocationChartSample {

    private static final Path SAMPLE_PPT =
            Path.of("samples/20260430-偏债混-M1.pptx");

    private static final List<String> CATEGORIES =
            List.of("股票", "可转债", "利率债", "信用债");

    private static final List<String> SERIES_NAMES =
            List.of("2025Q2", "2025Q3", "2025Q4", "2026Q1");

    /** 各季度下四类资产占比（%，约加总 100） */
    private static final List<List<Double>> SERIES_VALUES =
            List.of(
                    List.of(28.5, 22.0, 30.5, 19.0),
                    List.of(30.0, 20.5, 28.0, 21.5),
                    List.of(26.0, 24.0, 32.0, 18.0),
                    List.of(32.5, 18.5, 27.0, 22.0));

    public static void main(String[] args) throws Exception {
        Path ppt = SAMPLE_PPT.toAbsolutePath();
        if (!Files.isRegularFile(ppt)) {
            throw new IllegalStateException("样例不存在: " + ppt);
        }
        try (InputStream in = Files.newInputStream(ppt);
                XMLSlideShow show = new XMLSlideShow(in)) {
            XSLFSlide slide = show.getSlides().get(0);
            XSLFChart chart = findChart(slide, 1);
            ChartDataWriter.write(chart, CATEGORIES, SERIES_NAMES, SERIES_VALUES);
            try (OutputStream out = Files.newOutputStream(ppt)) {
                show.write(out);
            }
        }
        System.out.println("已更新图表数据: " + ppt);
        System.out.println("横轴: " + CATEGORIES);
        System.out.println("系列: " + SERIES_NAMES);
    }

    private static XSLFChart findChart(XSLFSlide slide, int ordinal) {
        int seen = 0;
        for (XSLFShape shape : ShapeWalker.collectDepthFirst(slide)) {
            if (shape instanceof XSLFGraphicFrame frame && frame.hasChart()) {
                seen++;
                if (seen == ordinal) {
                    return frame.getChart();
                }
            }
        }
        throw new IllegalStateException("未找到 chartOrdinal=" + ordinal);
    }

    private PatchAllocationChartSample() {}
}
