package com.example.pptrefresh.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartEmbeddedXlsxPatcherTest {

    @Test
    void patchKeepsXlsxSmall() throws Exception {
        Path source = Path.of("samples", "20260430-偏债混-M1.pptx");
        byte[] original;
        try (ZipFile zip = new ZipFile(source.toFile())) {
            original =
                    zip.getInputStream(zip.getEntry("ppt/embeddings/Microsoft_Excel_Sheet1.xlsx"))
                            .readAllBytes();
        }
        byte[] patched =
                ChartEmbeddedXlsxPatcher.patchBytes(
                        original,
                        new String[] {"2024Q2", "2024Q3", "2024Q4", "2025Q1"},
                        new String[] {"股票", "债券", "现金及其他"},
                        new double[][] {
                            {68.2, 71.5, 65.8, 72.4},
                            {22.5, 19.8, 24.1, 18.6},
                            {9.3, 8.7, 10.1, 9.0}
                        });
        assertTrue(patched.length < 8000, "patched xlsx should stay small: " + patched.length);
        String sheet = extractEntry(patched, "xl/worksheets/sheet1.xml");
        String shared = extractEntry(patched, "xl/sharedStrings.xml");
        assertTrue(shared.contains("2025Q1"));
        assertTrue(sheet.contains("<c r=\"B2\"><v>68.2</v></c>"), "stock value should be in column B");
        assertTrue(!sheet.contains("r=\"A2\"><v>90"), "must not overwrite category column A");
    }

    private static String extractEntry(byte[] zipBytes, String name) throws Exception {
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (name.equals(e.getName())) {
                    return new String(zin.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("missing " + name);
    }
}
