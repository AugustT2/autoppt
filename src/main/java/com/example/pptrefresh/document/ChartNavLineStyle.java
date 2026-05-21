package com.example.pptrefresh.document;

import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumFmt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.STAxPos;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBodyProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;
import org.apache.xmlbeans.XmlException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** 净值/累计收益折线图版式：去标题、纵轴百分点两位小数。 */
public final class ChartNavLineStyle {

    /** 数值已是「百分点」(如 3.2 表示 3.2%)，显示为 3.20% */
    public static final String PERCENT_POINT_FORMAT = "0.00\"%\"";

    /** 横轴刻度逆时针 90°（竖排，自下而上读） */
    private static final int CATEGORY_LABEL_ROT = -5400000;

    /** 坐标轴刻度字号（1/100 磅，800 = 8pt） */
    private static final int AXIS_LABEL_FONT_SZ = 800;

    private ChartNavLineStyle() {}

    /** 删除幻灯片上「累计收益率走势…」类图表标题文本框。 */
    public static void removeNavChartTitle(XSLFSlide slide) {
        List<XSLFShape> toRemove = new ArrayList<>();
        ShapeWalker.walkDepthFirst(
                slide,
                shape -> {
                    if (!(shape instanceof XSLFTextShape)) {
                        return;
                    }
                    XSLFTextShape text = (XSLFTextShape) shape;
                    String t = text.getText();
                    if (t != null && t.contains("累计收益率走势")) {
                        toRemove.add(shape);
                    }
                });
        for (XSLFShape shape : toRemove) {
            slide.removeShape(shape);
        }
    }

    /** 纵轴刻度带 %、保留两位小数；同步折线 numCache 格式。 */
    public static void applyPercentValAxis(XSLFChart chart) {
        CTChart ct = chart.getCTChart();
        if (ct == null) {
            return;
        }
        if (ct.isSetAutoTitleDeleted()) {
            ct.getAutoTitleDeleted().setVal(true);
        } else {
            ct.addNewAutoTitleDeleted().setVal(true);
        }
        CTPlotArea pa = ct.getPlotArea();
        if (pa == null) {
            return;
        }
        for (CTValAx ax : pa.getValAxList()) {
            if (ax.getAxPos() != null && ax.getAxPos().getVal() == STAxPos.L) {
                CTNumFmt fmt = ax.isSetNumFmt() ? ax.getNumFmt() : ax.addNewNumFmt();
                fmt.setFormatCode(PERCENT_POINT_FORMAT);
                fmt.setSourceLinked(false);
            }
        }
        for (CTLineChart lc : pa.getLineChartList()) {
            lc.getSerList()
                    .forEach(
                            ser -> {
                                if (ser.isSetVal()
                                        && ser.getVal().isSetNumRef()
                                        && ser.getVal().getNumRef().isSetNumCache()) {
                                    ser.getVal()
                                            .getNumRef()
                                            .getNumCache()
                                            .setFormatCode(PERCENT_POINT_FORMAT);
                                }
                            });
        }
    }

    /**
     * 横轴全量交易日 + 显示侧自动抽样：保留日标签格式与竖排小字号，并设置刻度间隔（约 10～12 个可见标签）。
     */
    public static void applyAutoSampledCategoryAxis(XSLFChart chart, int pointCount) {
        applyDailyCategoryAxis(chart);
        if (pointCount <= 0) {
            return;
        }
        CTPlotArea pa = chart.getCTChart() != null ? chart.getCTChart().getPlotArea() : null;
        if (pa == null) {
            return;
        }
        int skip = labelSkipForAutoSample(pointCount);
        for (CTCatAx ax : pa.getCatAxList()) {
            if (ax.getAxPos() == null || ax.getAxPos().getVal() != STAxPos.B) {
                continue;
            }
            if (ax.isSetAuto()) {
                ax.getAuto().setVal(true);
            } else {
                ax.addNewAuto().setVal(true);
            }
            patchTickLblSkip(ax, skip);
        }
    }

    private static final Pattern TICK_LBL_SKIP =
            Pattern.compile("<c:tickLblSkip val=\"\\d+\"/>");

    /** 约显示 12 个横轴刻度标签（POI lite 无 CTSkip，用 XML 补丁）。 */
    public static int labelSkipForAutoSample(int pointCount) {
        if (pointCount <= 20) {
            return 1;
        }
        return Math.max(2, (pointCount + 11) / 12);
    }

    private static void patchTickLblSkip(CTCatAx ax, int skip) {
        String xml = ax.xmlText();
        xml = TICK_LBL_SKIP.matcher(xml).replaceAll("");
        if (skip <= 1) {
            setCatAxXml(ax, xml);
            return;
        }
        String tag = "<c:tickLblSkip val=\"" + skip + "\"/>";
        int idx = xml.indexOf("<c:noMultiLvlLbl");
        if (idx < 0) {
            idx = xml.indexOf("noMultiLvlLbl");
        }
        if (idx >= 0) {
            int end = xml.indexOf("/>", idx);
            if (end > 0) {
                end += 2;
                xml = xml.substring(0, end) + tag + xml.substring(end);
                setCatAxXml(ax, xml);
                return;
            }
        }
        int insertAt = xml.lastIndexOf("</c:catAx>");
        if (insertAt < 0) {
            insertAt = xml.lastIndexOf("</catAx>");
        }
        if (insertAt < 0) {
            return;
        }
        xml = xml.substring(0, insertAt) + tag + xml.substring(insertAt);
        setCatAxXml(ax, xml);
    }

    private static void setCatAxXml(CTCatAx ax, String xml) {
        try {
            ax.set(CTCatAx.Factory.parse(xml));
        } catch (XmlException e) {
            throw new IllegalStateException("patch catAx", e);
        }
    }

    /**
     * 横轴：yyyy-MM-dd 文本、禁止多级折叠、刻度逆时针 90° 竖排、小号字体。
     */
    public static void applyDailyCategoryAxis(XSLFChart chart) {
        CTPlotArea pa = chart.getCTChart() != null ? chart.getCTChart().getPlotArea() : null;
        if (pa == null) {
            return;
        }
        for (CTCatAx ax : pa.getCatAxList()) {
            if (ax.getAxPos() == null || ax.getAxPos().getVal() != STAxPos.B) {
                continue;
            }
            if (ax.isSetNoMultiLvlLbl()) {
                ax.getNoMultiLvlLbl().setVal(true);
            } else {
                ax.addNewNoMultiLvlLbl().setVal(true);
            }
            CTTextBody txPr = ax.isSetTxPr() ? ax.getTxPr() : ax.addNewTxPr();
            CTTextBodyProperties bodyPr =
                    txPr.getBodyPr() != null ? txPr.getBodyPr() : txPr.addNewBodyPr();
            bodyPr.setRot(CATEGORY_LABEL_ROT);
            applyAxisLabelFont(txPr);
        }
    }

    private static void applyAxisLabelFont(CTTextBody txPr) {
        if (!txPr.isSetLstStyle()) {
            txPr.addNewLstStyle();
        }
        CTTextParagraph p = txPr.sizeOfPArray() > 0 ? txPr.getPArray(0) : txPr.addNewP();
        CTTextParagraphProperties pPr = p.isSetPPr() ? p.getPPr() : p.addNewPPr();
        CTTextCharacterProperties defRPr =
                pPr.isSetDefRPr() ? pPr.getDefRPr() : pPr.addNewDefRPr();
        defRPr.setSz(AXIS_LABEL_FONT_SZ);
        if (!p.isSetEndParaRPr()) {
            p.addNewEndParaRPr();
        }
    }
}
