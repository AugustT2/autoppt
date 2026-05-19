package com.example.pptrefresh.document;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** POI 能打开并另存 refreshed 样例，说明 OOXML 基本可读。 */
class PptxRoundTripTest {

    @Test
    void roundTripRefreshedSample() throws Exception {
        Path src = Path.of("samples", "20260430-\u504f\u503a\u6df7-M1-refreshed.pptx");
        Path out = Files.createTempFile("roundtrip-", ".pptx");
        try (InputStream in = Files.newInputStream(src);
                XMLSlideShow ppt = new XMLSlideShow(in);
                OutputStream os = Files.newOutputStream(out)) {
            ppt.write(os);
        }
        Files.deleteIfExists(out);
    }
}
