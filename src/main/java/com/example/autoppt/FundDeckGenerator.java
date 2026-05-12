package com.example.autoppt;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 从带「说明页 + 产品样板页」的 deck 生成多产品：先按样板复制幻灯片，再逐页填充。
 *
 * <p>要求：第 0 页为任意说明页保留；第 1 页为命名形状产品样板（勿先改内容再作为 import 源）。
 */
public final class FundDeckGenerator {

    private static final int PROTOTYPE_SLIDE_INDEX = 1;

    private FundDeckGenerator() {}

    public static void generate(Path templatePptx, Path outputPptx, List<FundProduct> funds)
            throws IOException, InvalidFormatException {
        if (funds.isEmpty()) {
            throw new IllegalArgumentException("至少一只基金");
        }
        try (InputStream in = Files.newInputStream(templatePptx);
                XMLSlideShow ppt = new XMLSlideShow(in)) {
            if (ppt.getSlides().size() <= PROTOTYPE_SLIDE_INDEX) {
                throw new IllegalStateException("模板至少包含 2 页（说明 + 产品样板）");
            }
            XSLFSlide prototype = ppt.getSlides().get(PROTOTYPE_SLIDE_INDEX);
            XSLFSlideLayout layout = prototype.getSlideLayout();

            List<XSLFSlide> productSlides = new ArrayList<>();
            productSlides.add(prototype);
            for (int i = 1; i < funds.size(); i++) {
                XSLFSlide copy = ppt.createSlide(layout);
                copy.importContent(prototype);
                productSlides.add(copy);
            }
            for (int i = 0; i < funds.size(); i++) {
                ProductSlideFiller.fill(productSlides.get(i), funds.get(i));
            }
            PresentationSlideSizeFix.fixWideScreenType(ppt);
            Path parent = outputPptx.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = outputPptx.resolveSibling(outputPptx.getFileName().toString() + ".writing");
            try (OutputStream out = Files.newOutputStream(temp)) {
                ppt.write(out);
            }
            Files.move(temp, outputPptx, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
