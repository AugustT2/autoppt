package com.example.pptrefresh.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** 对已落盘的 pptx 按路径替换 zip 内条目（避免 POI 序列化丢弃部分 chart 轴属性）。 */
public final class PptxZipPatcher {

    private static final Pattern TICK_LBL_SKIP =
            Pattern.compile("<c:tickLblSkip val=\"\\d+\"/>");

    private PptxZipPatcher() {}

    public static void replaceEntry(Path pptx, String entryPath, byte[] content) throws IOException {
        byte[] original = Files.readAllBytes(pptx);
        byte[] patched = patchEntry(original, entryPath, content);
        Files.write(pptx, patched);
    }

    public static void patchChart2CategoryLabelSkip(Path pptx, int skip) throws IOException {
        byte[] original = Files.readAllBytes(pptx);
        Map<String, byte[]> entries = unzip(original);
        byte[] chart = entries.get("ppt/charts/chart2.xml");
        if (chart == null) {
            return;
        }
        String xml = new String(chart, StandardCharsets.UTF_8);
        entries.put("ppt/charts/chart2.xml", patchCatAxTickLblSkip(xml, skip).getBytes(StandardCharsets.UTF_8));
        Files.write(pptx, zip(entries));
    }

    static String patchCatAxTickLblSkip(String chartXml, int skip) {
        String xml = TICK_LBL_SKIP.matcher(chartXml).replaceAll("");
        if (skip <= 1) {
            return xml;
        }
        String tag = "<c:tickLblSkip val=\"" + skip + "\"/>";
        Matcher m =
                Pattern.compile("<c:catAx[^>]*>[\\s\\S]*?</c:catAx>")
                        .matcher(xml);
        if (!m.find()) {
            return xml;
        }
        String catAx = m.group();
        String patched = insertAfterNoMultiLvlLbl(catAx, tag);
        return xml.substring(0, m.start()) + patched + xml.substring(m.end());
    }

    private static String insertAfterNoMultiLvlLbl(String catAx, String tag) {
        int idx = catAx.indexOf("<c:noMultiLvlLbl");
        if (idx < 0) {
            idx = catAx.indexOf("noMultiLvlLbl");
        }
        if (idx >= 0) {
            int end = catAx.indexOf("/>", idx);
            if (end > 0) {
                end += 2;
                return catAx.substring(0, end) + tag + catAx.substring(end);
            }
        }
        int insertAt = catAx.lastIndexOf("</c:catAx>");
        if (insertAt < 0) {
            return catAx;
        }
        return catAx.substring(0, insertAt) + tag + catAx.substring(insertAt);
    }

    private static byte[] patchEntry(byte[] zipBytes, String entryPath, byte[] content)
            throws IOException {
        Map<String, byte[]> entries = unzip(zipBytes);
        entries.put(entryPath, content);
        return zip(entries);
    }

    private static Map<String, byte[]> unzip(byte[] zipBytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                entries.put(entry.getName(), zin.readAllBytes());
            }
        }
        return entries;
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ZipOutputStream zout = new ZipOutputStream(bout)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                zout.putNextEntry(new ZipEntry(e.getKey()));
                zout.write(e.getValue());
                zout.closeEntry();
            }
        }
        return bout.toByteArray();
    }

}
