package com.example.autoppt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 入口：使用项目根目录下 {@code template_named_shapes_blank.pptx}，生成 {@code target/generated-fund-deck-out.pptx}
 * （若需固定路径可自行传参；写入先落临时文件再 replace，减少被占用失败概率）。
 *
 * <p>Plan（实现要点）：
 *
 * <ol>
 *   <li>模板：沿用 Python 生成的命名形状样板（第 1 页为产品页，第 0 页为说明）。</li>
 *   <li>复制：Apache POI {@code createSlide(layout) + importContent(prototype)}，在修改任一只基金前完成复制，
 *       避免把已填数据再拷进下一页。</li>
 *   <li>填充：按固定名称写文本框与表格（{@code org.apache.poi.xslf.usermodel.XSLFTable}）；表头/列数随 {@link FundProduct} 可变（演示 fundC 不同表头）。</li>
 *   <li>图表：{@link ChartSlideFiller} 对 {@code CHART_ALLOCATION} / {@code CHART_NAV_SERIES} 使用 XDDF
 *       {@code Series.replaceData} 替换分类与数值（系列数需与模板一致）。</li>
 *   <li>校验：缺失命名形状时抛错，便于 CI 做契约测试。</li>
 * </ol>
 */
public final class FundDeckGeneratorApp {

    public static void main(String[] args) throws Exception {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path template = projectDir.resolve("template_named_shapes_blank.pptx");
        Path out =
                args.length >= 1
                        ? Paths.get(args[0])
                        : projectDir.resolve("target").resolve("generated-fund-deck-out.pptx");
        Files.createDirectories(out.getParent());
        FundDeckGenerator.generate(template, out, SampleFunds.all());
        System.out.println("Written: " + out.toAbsolutePath());
    }
}
