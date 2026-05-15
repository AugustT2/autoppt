package com.example.pptrefresh.failure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FailureReportWriter {

    private final ObjectMapper mapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public Path failedJsonPath(Path expectedOutputPptx) {
        String name = expectedOutputPptx.getFileName().toString();
        if (!name.toLowerCase().endsWith(".pptx")) {
            return expectedOutputPptx.resolveSibling(name + ".failed.json");
        }
        String base = name.substring(0, name.length() - ".pptx".length());
        return expectedOutputPptx.resolveSibling(base + ".failed.json");
    }

    public void write(Path expectedOutputPptx, FailureReport report) throws IOException {
        Path out = failedJsonPath(expectedOutputPptx);
        Files.createDirectories(out.getParent());
        mapper.writeValue(out.toFile(), report);
    }
}
