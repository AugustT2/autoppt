package com.example.pptrefresh.document;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xslf.usermodel.XSLFChart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 在不使用 POI {@code saveWorkbook} 的前提下，按模板布局补丁嵌入 xlsx，避免体积膨胀且让 PowerPoint 读到新数据。
 *
 * <p>布局：A 列=分类（季度），B..=系列（资产类别）；第 1 行=系列名。
 */
final class ChartEmbeddedXlsxPatcher {

    private ChartEmbeddedXlsxPatcher() {}

    static void patch(
            XSLFChart chart,
            String[] categories,
            String[] seriesNames,
            double[][] seriesValues)
            throws IOException {
        PackagePart embed = findEmbedPart(chart);
        if (embed == null) {
            return;
        }
        byte[] original;
        try (InputStream in = embed.getInputStream()) {
            original = in.readAllBytes();
        }
        byte[] patched = patchBytes(original, categories, seriesNames, seriesValues);
        try (OutputStream out = embed.getOutputStream()) {
            out.write(patched);
        }
    }

    private static PackagePart findEmbedPart(XSLFChart chart) {
        for (POIXMLDocumentPart.RelationPart rp : chart.getRelationParts()) {
            String type = rp.getRelationship().getRelationshipType();
            if (type != null && type.contains("/package")) {
                POIXMLDocumentPart part = rp.getDocumentPart();
                return part != null ? part.getPackagePart() : null;
            }
        }
        return null;
    }

    static byte[] patchBytes(
            byte[] xlsxBytes, String[] categories, String[] seriesNames, double[][] seriesValues)
            throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(xlsxBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                entries.put(entry.getName(), zin.readAllBytes());
            }
        }

        List<String> shared = readSharedStrings(entries.get("xl/sharedStrings.xml"));
        List<String> needed = new ArrayList<>();
        for (String s : seriesNames) {
            addUnique(needed, s);
        }
        for (String c : categories) {
            addUnique(needed, c);
        }
        for (String s : needed) {
            addUnique(shared, s);
        }

        entries.put("xl/sharedStrings.xml", buildSharedStrings(shared));
        int lastRow = 1 + categories.length;
        // 模板布局：A 列=季度，B..=系列（表头在 B1 而非 A1）
        String lastCol = colLetter(seriesNames.length + 1);
        entries.put(
                "xl/worksheets/sheet1.xml",
                buildSheet1(categories, seriesNames, seriesValues, shared, lastRow, lastCol));

        return zipEntries(entries);
    }

    private static void addUnique(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    private static List<String> readSharedStrings(byte[] xml) {
        List<String> list = new ArrayList<>();
        if (xml == null) {
            return list;
        }
        String s = new String(xml, StandardCharsets.UTF_8);
        int idx = 0;
        while (true) {
            int start = s.indexOf("<t>", idx);
            if (start < 0) {
                break;
            }
            int end = s.indexOf("</t>", start);
            if (end < 0) {
                break;
            }
            list.add(s.substring(start + 3, end));
            idx = end + 4;
        }
        return list;
    }

    private static byte[] buildSharedStrings(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"");
        sb.append(" count=\"").append(strings.size()).append("\" uniqueCount=\"").append(strings.size()).append("\">");
        for (String t : strings) {
            sb.append("<si><t>").append(escapeXml(t)).append("</t></si>");
        }
        sb.append("</sst>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] buildSheet1(
            String[] categories,
            String[] seriesNames,
            double[][] seriesValues,
            List<String> shared,
            int lastRow,
            String lastCol) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<dimension ref=\"A1:").append(lastCol).append(lastRow).append("\"/>");
        sb.append("<sheetData>");
        sb.append("<row r=\"1\">");
        for (int s = 0; s < seriesNames.length; s++) {
            sb.append(stringCell(seriesCol(s), 1, shared.indexOf(seriesNames[s])));
        }
        sb.append("</row>");
        for (int i = 0; i < categories.length; i++) {
            int row = 2 + i;
            sb.append("<row r=\"").append(row).append("\">");
            sb.append(stringCell("A", row, shared.indexOf(categories[i])));
            for (int s = 0; s < seriesNames.length; s++) {
                sb.append(numCell(seriesCol(s), row, seriesValues[s][i]));
            }
            sb.append("</row>");
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String stringCell(String col, int row, int sharedIndex) {
        return "<c r=\"" + col + row + "\" t=\"s\"><v>" + sharedIndex + "</v></c>";
    }

    private static String numCell(String col, int row, double value) {
        return "<c r=\"" + col + row + "\"><v>" + value + "</v></c>";
    }

    /** 第 s 个系列对应列 B/C/D…（s=0 → B）。 */
    private static String seriesCol(int seriesIndex) {
        return colLetter(seriesIndex + 2);
    }

    private static String colLetter(int col1Based) {
        return String.valueOf((char) ('A' + col1Based - 1));
    }

    private static String escapeXml(String t) {
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static byte[] zipEntries(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zout = new ZipOutputStream(bos)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey());
                zout.putNextEntry(ze);
                zout.write(e.getValue());
                zout.closeEntry();
            }
        }
        return bos.toByteArray();
    }
}
