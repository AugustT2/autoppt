package com.example.pptrefresh.sample;

import com.example.pptrefresh.PptRefreshApplication;
import com.example.pptrefresh.orchestration.RefreshJobRequest;
import com.example.pptrefresh.orchestration.RefreshJobResult;
import com.example.pptrefresh.orchestration.RefreshOrchestrator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地联调完整刷新链路（需配置 LLM API，如环境变量 {@code DASHSCOPE_API_KEY}）。
 *
 * <p>IDEA 直接运行 main，或：
 * <pre>
 * mvn -q compile exec:java "-Dexec.mainClass=com.example.pptrefresh.sample.RefreshSampleMain"
 * </pre>
 */
public final class RefreshSampleMain {

    private RefreshSampleMain() {}

    public static void main(String[] args) throws Exception {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path source =
                args.length >= 1
                        ? Paths.get(args[0])
                        : projectDir.resolve("samples").resolve("20260430-偏债混-M1.pptx");
        Path output =
                args.length >= 2
                        ? Paths.get(args[1])
                        : projectDir.resolve("samples").resolve("20260430-偏债混-M1-refreshed.pptx");
        if (args.length < 2 && Files.exists(output)) {
            try {
                java.nio.channels.FileChannel.open(
                        output,
                        java.nio.file.StandardOpenOption.WRITE,
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception locked) {
                output = projectDir.resolve("samples").resolve("20260430-偏债混-M1-refreshed-new.pptx");
                System.out.println("默认输出文件被占用，改写入: " + output.toAbsolutePath());
            }
        }

        if (!Files.exists(source)) {
            System.err.println("源文件不存在，请将样例 pptx 放到: " + source.toAbsolutePath());
            System.exit(1);
        }

        SpringApplication app = new SpringApplication(PptRefreshApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);

        try (ConfigurableApplicationContext ctx = app.run()) {
            RefreshOrchestrator orchestrator = ctx.getBean(RefreshOrchestrator.class);
            RefreshJobRequest request = new RefreshJobRequest();
            request.setSourcePptxPath(source.toAbsolutePath().toString());
            request.setOutputPptxPath(output.toAbsolutePath().toString());

            RefreshJobResult result = orchestrator.run(request);
            if (result.isSuccess()) {
                System.out.println("刷新成功: " + result.getOutputPptxPath());
            } else {
                System.err.println("刷新失败: " + result.getMessage());
                if (result.getFailedJsonPath() != null) {
                    System.err.println("报告: " + result.getFailedJsonPath());
                }
                System.exit(1);
            }
        }
    }
}
