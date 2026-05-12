package com.example.autoppt;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.IOException;
import java.util.List;

/**
 * 将 {@link FundProduct} 写入命名形状（与 {@code build_template_demos.py} 生成的模板一致）。
 *
 * <p>约定名称：TITLE_PAGE, LBL_FUND_SECTION, TXT_FUND_PROFILE, LBL_RANK_SECTION, TBL_RANKING,
 * LBL_ALLOC_SECTION, LBL_NAV_SECTION；图表由 {@link ChartSlideFiller} 写入。
 */
public final class ProductSlideFiller {

    public static final String TITLE_PAGE = "TITLE_PAGE";
    public static final String LBL_FUND_SECTION = "LBL_FUND_SECTION";
    public static final String TXT_FUND_PROFILE = "TXT_FUND_PROFILE";
    public static final String LBL_RANK_SECTION = "LBL_RANK_SECTION";
    public static final String TBL_RANKING = "TBL_RANKING";
    public static final String LBL_ALLOC_SECTION = "LBL_ALLOC_SECTION";
    public static final String LBL_NAV_SECTION = "LBL_NAV_SECTION";

    private ProductSlideFiller() {}

    public static void fill(XSLFSlide slide, FundProduct fund) throws IOException, InvalidFormatException {
        setTextShape(slide, TITLE_PAGE, fund.titleLine());
        setTextShape(slide, LBL_FUND_SECTION, fund.sectionFundLabel());
        setMultilineTextShape(slide, TXT_FUND_PROFILE, fund.profileLines());
        setTextShape(slide, LBL_RANK_SECTION, fund.sectionRankLabel());
        fillTable(slide, TBL_RANKING, fund.tableHeaders(), fund.tableRows());
        setTextShape(slide, LBL_ALLOC_SECTION, fund.sectionAllocLabel());
        setTextShape(slide, LBL_NAV_SECTION, fund.sectionNavLabel());
        ChartSlideFiller.fillCharts(slide, fund.charts());
    }

    private static void setTextShape(XSLFSlide slide, String shapeName, String text) {
        XSLFShape sh = ShapeFinder.find(slide, shapeName);
        if (sh == null) {
            throw new IllegalStateException("缺少命名形状: " + shapeName);
        }
        if (!(sh instanceof XSLFTextShape)) {
            throw new IllegalStateException(shapeName + " 不是文本框: " + sh.getClass());
        }
        XSLFTextShape tx = (XSLFTextShape) sh;
        tx.clearText();
        XSLFTextRun run = tx.setText(text);
        run.setFontFamily("微软雅黑");
    }

    private static void setMultilineTextShape(XSLFSlide slide, String shapeName, List<String> lines) {
        XSLFShape sh = ShapeFinder.find(slide, shapeName);
        if (sh == null) {
            throw new IllegalStateException("缺少命名形状: " + shapeName);
        }
        if (!(sh instanceof XSLFTextShape)) {
            throw new IllegalStateException(shapeName + " 不是文本框: " + sh.getClass());
        }
        XSLFTextShape tx = (XSLFTextShape) sh;
        tx.clearText();
        if (lines.isEmpty()) {
            return;
        }
        XSLFTextParagraph firstP =
                tx.getTextParagraphs().isEmpty() ? tx.addNewTextParagraph() : tx.getTextParagraphs().get(0);
        for (int i = 0; i < lines.size(); i++) {
            XSLFTextParagraph p = (i == 0) ? firstP : tx.addNewTextParagraph();
            XSLFTextRun run = p.addNewTextRun();
            run.setText(lines.get(i));
            run.setFontFamily("微软雅黑");
            run.setFontSize(10.0);
        }
        firstP.getTextRuns().get(0).setBold(true);
    }

    private static void fillTable(
            XSLFSlide slide, String shapeName, List<String> headers, List<List<String>> rows) {
        XSLFShape sh = ShapeFinder.find(slide, shapeName);
        if (sh == null) {
            throw new IllegalStateException("缺少命名形状: " + shapeName);
        }
        if (!(sh instanceof XSLFTable)) {
            throw new IllegalStateException(shapeName + " 不是表格: " + sh.getClass());
        }
        XSLFTable table = (XSLFTable) sh;
        int needRows = 1 + rows.size();
        int needCols = headers.size();
        while (table.getNumberOfRows() < needRows) {
            table.addRow();
        }
        while (table.getNumberOfRows() > needRows) {
            table.removeRow(table.getNumberOfRows() - 1);
        }
        while (table.getNumberOfColumns() < needCols) {
            table.addColumn();
        }
        while (table.getNumberOfColumns() > needCols) {
            table.removeColumn(table.getNumberOfColumns() - 1);
        }
        for (int c = 0; c < needCols; c++) {
            setCell(table.getCell(0, c), headers.get(c), true);
        }
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < needCols; c++) {
                String val = c < row.size() ? row.get(c) : "";
                setCell(table.getCell(r + 1, c), val, false);
            }
        }
    }

    private static void setCell(XSLFTableCell cell, String text, boolean header) {
        cell.setText(text);
        for (XSLFTextParagraph p : cell.getTextParagraphs()) {
            for (XSLFTextRun run : p.getTextRuns()) {
                run.setFontFamily("微软雅黑");
                run.setBold(header);
                run.setFontSize(header ? 9.0 : 9.0);
            }
        }
    }
}
