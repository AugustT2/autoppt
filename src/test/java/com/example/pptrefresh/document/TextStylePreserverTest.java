package com.example.pptrefresh.document;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextStylePreserverTest {

  private static final Pattern DEF_RPR_BOLD =
      Pattern.compile("<a:defRPr[^>]*\\sb=\"([01])\"");

  @Test
  void multiParagraphWriteKeepsPerParagraphBold() throws Exception {
    Path sample = Path.of("samples", "20260430-偏债混-M1.pptx");
    try (InputStream in = sample.toFile().toURI().toURL().openStream();
        XMLSlideShow ppt = new XMLSlideShow(in)) {
      XSLFSlide slide = ppt.getSlides().get(0);
      XSLFTextShape fundMeta = findShapeContaining(slide, "基金类型");
      String replacement =
          "蓝海稳健增长混合A（示例）\n"
              + "基金类型：偏股混合型基金\n"
              + "成立日期：2019-06-12　　最新规模：58.6 亿元（示例）\n"
              + "基金经理：张明、李悦（示例）\n"
              + "业绩比较基准：沪深300指数收益率×70% + 中债综合指数×30%\n"
              + "风险等级：R3（中风险）　　托管人：示例商业银行";
      TextStylePreserver.setShapeText(fundMeta, "基金名称：" + replacement);

      String xml = fundMeta.getXmlObject().toString();
      Matcher m = DEF_RPR_BOLD.matcher(xml);
      int para = 0;
      while (m.find()) {
        String bold = m.group(1);
        if (para == 0) {
          assertEquals("1", bold, "first line (fund name) may be bold");
        } else {
          assertEquals("0", bold, "paragraph " + para + " should not be bold");
        }
        para++;
      }
      assertTrue(para >= 6, "expected at least 6 paragraphs");
    }
  }

  private static XSLFTextShape findShapeContaining(XSLFSlide slide, String needle) {
    for (var shape : slide.getShapes()) {
      if (shape instanceof XSLFTextShape text
          && text.getText() != null
          && text.getText().contains(needle)) {
        return text;
      }
    }
    throw new AssertionError("shape not found: " + needle);
  }
}
