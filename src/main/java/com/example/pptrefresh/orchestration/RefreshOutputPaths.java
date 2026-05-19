package com.example.pptrefresh.orchestration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 刷新结果路径：禁止覆盖源文件，必要时自动生成 {@code *-refreshed.pptx}。 */
final class RefreshOutputPaths {

    private RefreshOutputPaths() {}

    static final class Resolved {
        private final Path source;
        private final Path output;
        private final boolean outputAutoDerived;

        Resolved(Path source, Path output, boolean outputAutoDerived) {
            this.source = source;
            this.output = output;
            this.outputAutoDerived = outputAutoDerived;
        }

        Path source() {
            return source;
        }

        Path output() {
            return output;
        }

        boolean outputAutoDerived() {
            return outputAutoDerived;
        }
    }

    static Resolved resolve(Path sourcePath, Path outputPath) throws IOException {
        Path source = sourcePath.toAbsolutePath().normalize();
        Path output = outputPath.toAbsolutePath().normalize();
        if (!Files.exists(source)) {
            throw new IOException("源文件不存在: " + source);
        }
        boolean autoDerived = false;
        if (isSameFile(source, output)) {
            output = defaultRefreshOutput(source);
            autoDerived = true;
        }
        return new Resolved(source, output, autoDerived);
    }

    static Path defaultRefreshOutput(Path source) {
        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return source.resolveSibling(fileName + "-refreshed");
        }
        String base = fileName.substring(0, dot);
        String ext = fileName.substring(dot);
        return source.resolveSibling(base + "-refreshed" + ext);
    }

    private static boolean isSameFile(Path a, Path b) {
        try {
            return Files.isSameFile(a, b);
        } catch (IOException e) {
            return a.equals(b);
        }
    }
}
