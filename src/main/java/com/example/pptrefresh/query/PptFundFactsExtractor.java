package com.example.pptrefresh.query;

import com.example.pptrefresh.document.ShapeWalker;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从 PPT 正文扫描成立日、基金经理任职日（演示级正则）。 */
public final class PptFundFactsExtractor {

    private static final Pattern INCEPTION =
            Pattern.compile("成立日期[：:]\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern TENURE =
            Pattern.compile("任职日期[：:]\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private PptFundFactsExtractor() {}

    public static Optional<LocalDate> readInceptionDate(XMLSlideShow ppt) {
        return scanAllText(ppt).stream()
                .map(PptFundFactsExtractor::matchInception)
                .flatMap(Optional::stream)
                .findFirst();
    }

    public static Optional<LocalDate> readManagerTenureStart(
            XMLSlideShow ppt, ManagerTenureRule rule) {
        List<LocalDate> tenures = new ArrayList<>();
        for (String block : scanAllText(ppt)) {
            Matcher m = TENURE.matcher(block);
            while (m.find()) {
                tenures.add(LocalDate.parse(m.group(1), ISO));
            }
        }
        if (tenures.isEmpty()) {
            return Optional.empty();
        }
        if (rule == ManagerTenureRule.LATEST) {
            return tenures.stream().max(LocalDate::compareTo);
        }
        return tenures.stream().min(LocalDate::compareTo);
    }

    private static Optional<LocalDate> matchInception(String text) {
        Matcher m = INCEPTION.matcher(text);
        if (m.find()) {
            return Optional.of(LocalDate.parse(m.group(1), ISO));
        }
        return Optional.empty();
    }

    private static List<String> scanAllText(XMLSlideShow ppt) {
        List<String> blocks = new ArrayList<>();
        for (XSLFSlide slide : ppt.getSlides()) {
            ShapeWalker.walkDepthFirst(
                    slide,
                    shape -> {
                        if (shape instanceof XSLFTextShape) {
                            String t = ((XSLFTextShape) shape).getText();
                            if (t != null && !t.isBlank()) {
                                blocks.add(t);
                            }
                        }
                    });
        }
        return blocks;
    }
}
