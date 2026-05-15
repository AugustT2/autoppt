package com.example.pptrefresh.naming;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;

import java.nio.file.Path;

public final class FilenameParser {

    private FilenameParser() {}

    public static ParsedFilename parse(Path pptxPath) {
        String fileName = pptxPath.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".pptx")) {
            throw new RefreshException(
                    FailureStage.FILENAME_PARSE,
                    "INVALID_EXTENSION",
                    "文件名必须以 .pptx 结尾: " + fileName);
        }
        String basename = fileName.substring(0, fileName.length() - ".pptx".length());
        String[] segments = basename.split("-", -1);
        if (segments.length != 3) {
            throw new RefreshException(
                    FailureStage.FILENAME_PARSE,
                    "INVALID_SEGMENT_COUNT",
                    "文件名按 '-' 切分必须恰好 3 段，实际 " + segments.length + ": " + basename);
        }
        String deckType = segments[1] + "-" + segments[2];
        return new ParsedFilename(segments[0], segments[1], segments[2], deckType);
    }
}
