package com.example.pptrefresh.sample;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 从备份恢复样例 PPT，再依次刷新资产配置图与净值折线图（避免 chart 系列缺 order 导致打不开）。
 *
 * <p>{@code mvn -q compile exec:java -Dexec.mainClass=com.example.pptrefresh.sample.PatchDeckChartsSample}
 */
public final class PatchDeckChartsSample {

    private static final Path SAMPLE =
            Path.of("samples/20260430-偏债混-M1.pptx");
    private static final Path BACKUP =
            Path.of("samples/20260430-偏债混-M1——backup.pptx");

    public static void main(String[] args) throws Exception {
        Path backup = BACKUP.toAbsolutePath();
        Path ppt = SAMPLE.toAbsolutePath();
        if (!Files.isRegularFile(backup)) {
            throw new IllegalStateException("备份不存在: " + backup);
        }
        Files.copy(backup, ppt, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("已从备份恢复: " + ppt);
        PatchAllocationChartSample.main(args);
        PatchNavChartSample.main(args);
        System.out.println("整页图表已刷新: " + ppt);
    }

    private PatchDeckChartsSample() {}
}
