package com.example.pptrefresh.sample;

import com.example.pptrefresh.document.ChartDataWriter;
import com.example.pptrefresh.document.ChartNavLineStyle;
import com.example.pptrefresh.document.PptxZipPatcher;
import com.example.pptrefresh.document.ShapeWalker;
import com.example.pptrefresh.time.TradingDayCalendar;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 刷新样例 PPT 下方累计收益折线图：嵌入表/strCache 为全量交易日，横轴显示由 PPT 自动抽样。
 *
 * <p>{@code mvn -q compile exec:java -Dexec.mainClass=com.example.pptrefresh.sample.PatchNavChartSample}
 */
public final class PatchNavChartSample {

    private static final Path SAMPLE_PPT =
            Path.of("samples/20260430-偏债混-M1.pptx");

    private static final String FUND_NAME = "蓝海稳健增长混合A";
    private static final String BENCH_NAME = "偏债混合基金指数";

    /** 与文件名截止日一致：近一年交易日（全量写入，显示抽样）。 */
    private static final LocalDate RANGE_END = LocalDate.of(2026, 4, 30);
    private static final LocalDate RANGE_START = RANGE_END.minusYears(1).plusDays(1);

    public static void main(String[] args) throws Exception {
        Path ppt = SAMPLE_PPT.toAbsolutePath();
        Path out = ppt;
        if (!Files.isRegularFile(ppt)) {
            throw new IllegalStateException("样例不存在: " + ppt);
        }
        List<String> categories = TradingDayCalendar.labelsBetween(RANGE_START, RANGE_END);
        List<Double> fund = demoReturns(categories.size(), 42, 6.5);
        List<Double> bench = demoReturns(categories.size(), 43, 5.8);
        for (int i = 1; i < bench.size(); i++) {
            if (bench.get(i) > fund.get(i)) {
                bench.set(i, Math.max(0, round2(fund.get(i) - 0.15)));
            }
        }
        List<String> seriesNames = List.of(FUND_NAME, BENCH_NAME);
        List<List<Double>> seriesValues = List.of(fund, bench);

        try (InputStream in = Files.newInputStream(ppt);
                XMLSlideShow show = new XMLSlideShow(in)) {
            XSLFSlide slide = show.getSlides().get(0);
            ChartNavLineStyle.removeNavChartTitle(slide);
            XSLFChart chart = findChart(slide, 2);
            ChartDataWriter.write(chart, categories, seriesNames, seriesValues);
            ChartNavLineStyle.applyPercentValAxis(chart);
            ChartNavLineStyle.applyAutoSampledCategoryAxis(chart, categories.size());
            Path tmp =
                    ppt.resolveSibling(
                            ppt.getFileName().toString().replace(".pptx", "-nav-styled.pptx"));
            try (OutputStream os = Files.newOutputStream(tmp)) {
                show.write(os);
            }
            Path saved = tmp;
            try {
                Files.move(tmp, ppt, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                saved = ppt;
            } catch (IOException locked) {
                out = tmp;
                saved = tmp;
                System.out.println("原文件被占用，请关闭 PPT 后覆盖；已写入: " + tmp);
            }
            int labelSkip = ChartNavLineStyle.labelSkipForAutoSample(categories.size());
            PptxZipPatcher.patchChart2CategoryLabelSkip(saved, labelSkip);
        }
        System.out.println("已更新: " + out.toAbsolutePath());
        System.out.println(
                "横轴全量交易日: "
                        + categories.size()
                        + " 个 ("
                        + categories.get(0)
                        + " … "
                        + categories.get(categories.size() - 1)
                        + ")");
        System.out.println(
                "显示抽样间隔 tickLblSkip="
                        + ChartNavLineStyle.labelSkipForAutoSample(categories.size()));
        System.out.println("系列: " + seriesNames);
    }

    private static List<Double> demoReturns(int n, long seed, double cap) {
        Random r = new Random(seed);
        List<Double> vals = new ArrayList<>();
        vals.add(0.0);
        for (int i = 1; i < n; i++) {
            double next = Math.min(cap, vals.get(i - 1) + r.nextDouble() * 0.3 + 0.05);
            vals.add(round2(next));
        }
        return vals;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static XSLFChart findChart(XSLFSlide slide, int ordinal) {
        int seen = 0;
        for (XSLFShape shape : ShapeWalker.collectDepthFirst(slide)) {
            if (shape instanceof XSLFGraphicFrame) {
                XSLFGraphicFrame frame = (XSLFGraphicFrame) shape;
                if (frame.hasChart()) {
                    seen++;
                    if (seen == ordinal) {
                        return frame.getChart();
                    }
                }
            }
        }
        throw new IllegalStateException("未找到 chartOrdinal=" + ordinal);
    }

    private PatchNavChartSample() {}
}
