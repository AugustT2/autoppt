package com.example.pptrefresh.document;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 二分定位「打不开」根因：对比嵌入 xlsx 体积是否在 POI 保存后被重写膨胀。
 */
class PptxIntegrityProbeTest {

    private static final Path SOURCE = Path.of("samples", "20260430-偏债混-M1.pptx");

    @Test
    void probeSaveStrategies(@TempDir Path temp) throws Exception {
        assertTrue(Files.exists(SOURCE), "missing " + SOURCE.toAbsolutePath());

        Path copyOnly = temp.resolve("01-copy-only.pptx");
        Files.copy(SOURCE, copyOnly, StandardCopyOption.REPLACE_EXISTING);
        logEmbedding("01-copy-only", copyOnly);

        Path openCloseOpc = temp.resolve("02-opc-open-close.pptx");
        Files.copy(SOURCE, openCloseOpc, StandardCopyOption.REPLACE_EXISTING);
        try (PptPackageSession session = PptPackageSession.open(openCloseOpc)) {
            // no edits
        }
        logEmbedding("02-opc-open-close", openCloseOpc);

        Path streamWrite = temp.resolve("03-stream-write.pptx");
        Files.copy(SOURCE, streamWrite, StandardCopyOption.REPLACE_EXISTING);
        try (InputStream in = Files.newInputStream(streamWrite);
                XMLSlideShow ppt = new XMLSlideShow(in);
                OutputStream out = Files.newOutputStream(streamWrite)) {
            ppt.write(out);
        }
        logEmbedding("03-stream-write", streamWrite);

        System.out.println("Probe files under: " + temp.toAbsolutePath());
        System.out.println("手动用 PowerPoint 打开 01/02/03，判断从哪一步开始损坏。");
    }

    private static void logEmbedding(String label, Path pptx) throws Exception {
        try (ZipFile zip = new ZipFile(pptx.toFile())) {
            long s1 = size(zip, "ppt/embeddings/Microsoft_Excel_Sheet1.xlsx");
            long s2 = size(zip, "ppt/embeddings/Microsoft_Excel_Sheet2.xlsx");
            System.out.printf("%s embed1=%d embed2=%d (source ~5518/5529)%n", label, s1, s2);
        }
    }

    private static long size(ZipFile zip, String name) {
        ZipEntry e = zip.getEntry(name);
        return e != null ? e.getSize() : -1;
    }
}
