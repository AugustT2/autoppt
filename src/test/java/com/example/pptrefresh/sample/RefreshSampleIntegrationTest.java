package com.example.pptrefresh.sample;

import com.example.pptrefresh.orchestration.RefreshJobRequest;
import com.example.pptrefresh.orchestration.RefreshJobResult;
import com.example.pptrefresh.orchestration.RefreshOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("stub-llm")
class RefreshSampleIntegrationTest {

    private static final Pattern QUARTER = Pattern.compile("20\\d{2}Q\\d");

    @Autowired RefreshOrchestrator orchestrator;

    @Test
    void refreshSamplePptxProducesValidCharts(@TempDir Path temp) throws Exception {
        Path source = Path.of("samples", "20260430-偏债混-M1.pptx");
        assertTrue(Files.exists(source), "missing sample: " + source.toAbsolutePath());

        Path output = temp.resolve("out-refreshed.pptx");
        RefreshJobRequest request = new RefreshJobRequest();
        request.setSourcePptxPath(source.toAbsolutePath().toString());
        request.setOutputPptxPath(output.toAbsolutePath().toString());

        RefreshJobResult result = orchestrator.run(request);
        assertTrue(result.success(), () -> "refresh failed: " + result.message());
        assertTrue(Files.exists(output));

        assertAllocationChartOrientation(output);
        assertEmbeddingsNotRewritten(output);
        assertTitleRefreshed(output);
    }

    private static void assertTitleRefreshed(Path pptx) throws Exception {
        try (ZipFile zip = new ZipFile(pptx.toFile())) {
            String slide =
                    new String(
                            zip.getInputStream(zip.getEntry("ppt/slides/slide1.xml")).readAllBytes(),
                            StandardCharsets.UTF_8);
            assertTrue(
                    slide.contains("蓝海稳健增长混合A"),
                    "title should be written back with resolved product name");
        }
    }

    private static void assertEmbeddingsNotRewritten(Path pptx) throws Exception {
        try (ZipFile zip = new ZipFile(pptx.toFile())) {
            long s1 = zip.getEntry("ppt/embeddings/Microsoft_Excel_Sheet1.xlsx").getSize();
            long s2 = zip.getEntry("ppt/embeddings/Microsoft_Excel_Sheet2.xlsx").getSize();
            assertTrue(s1 < 8000 && s2 < 8000, () -> "embed xlsx inflated: " + s1 + ", " + s2);
        }
    }

    /** 柱图：系列名=资产类别，分类轴=季度（与模板嵌入表一致）。 */
    private static void assertAllocationChartOrientation(Path pptx) throws Exception {
        try (ZipFile zip = new ZipFile(pptx.toFile())) {
            String chart1 =
                    new String(
                            zip.getInputStream(zip.getEntry("ppt/charts/chart1.xml")).readAllBytes(),
                            StandardCharsets.UTF_8);
            assertSeriesTitleIsAsset(chart1);
            assertCategoryAxisUsesQuarters(chart1);
            assertFormulaCacheAligned(chart1);
        }
    }

    private static void assertSeriesTitleIsAsset(String chartXml) {
        int idx = chartXml.indexOf("<c:strRef>");
        assertTrue(idx >= 0);
        int end = chartXml.indexOf("</c:tx>", idx);
        String titleRef = chartXml.substring(idx, end > idx ? end : idx + 400);
        assertTrue(titleRef.contains("Sheet1!$B$1"), "first series title should reference B$1");
        assertEquals(1, countMatches(titleRef, "<c:pt "), "series title should have one point");
    }

    private static int countMatches(String text, String needle) {
        int n = 0, i = 0;
        while ((i = text.indexOf(needle, i)) >= 0) {
            n++;
            i += needle.length();
        }
        return n;
    }

    /** 分类轴公式行数与 ptCount 一致（避免 PowerPoint 修复对话框）。 */
    private static void assertFormulaCacheAligned(String chartXml) {
        Pattern block =
                Pattern.compile(
                        "Sheet1!\\$A\\$2:\\$A\\$(\\d+)</c:f><c:strCache><c:ptCount val=\"(\\d+)\"");
        var m = block.matcher(chartXml);
        assertTrue(m.find(), "missing category axis block");
        int endRow = Integer.parseInt(m.group(1));
        int ptCount = Integer.parseInt(m.group(2));
        assertEquals(endRow - 2 + 1, ptCount, "category formula rows vs ptCount");
    }

    private static void assertCategoryAxisUsesQuarters(String chartXml) {
        int catIdx = chartXml.indexOf("Sheet1!$A$2");
        assertTrue(catIdx >= 0, "missing category range");
        String snippet = chartXml.substring(catIdx, Math.min(catIdx + 600, chartXml.length()));
        assertTrue(
                QUARTER.matcher(snippet).find(),
                "category axis should contain quarter labels, snippet=" + snippet);
    }
}
