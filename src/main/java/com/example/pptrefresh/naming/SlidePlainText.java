package com.example.pptrefresh.naming;

import com.example.pptrefresh.document.ShapeWalker;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

/** 从幻灯片抽取纯文本，供产品名解析等使用。 */
public final class SlidePlainText {

    private SlidePlainText() {}

    public static String collectSlide(XMLSlideShow ppt, int slideIndexBase, int slideIndex) {
        int idx = slideIndexBase + slideIndex;
        if (idx < 0 || idx >= ppt.getSlides().size()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        ShapeWalker.walkDepthFirst(
                ppt.getSlides().get(idx),
                shape -> {
                    if (shape instanceof XSLFTextShape) {
                        String t = ((XSLFTextShape) shape).getText();
                        if (t != null && !t.isBlank()) {
                            if (sb.length() > 0) {
                                sb.append('\n');
                            }
                            sb.append(t.trim());
                        }
                    }
                });
        return sb.toString();
    }

    public static String collectSlides(XMLSlideShow ppt, int slideIndexBase, Iterable<Integer> slideIndexes) {
        StringBuilder sb = new StringBuilder();
        for (Integer s : slideIndexes) {
            String chunk = collectSlide(ppt, slideIndexBase, s);
            if (!chunk.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("\n---\n");
                }
                sb.append(chunk);
            }
        }
        return sb.toString();
    }
}
