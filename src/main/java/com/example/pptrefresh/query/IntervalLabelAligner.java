package com.example.pptrefresh.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将 LLM 返回的区间标签对齐到矩阵单元格原文（词表同义词如「过去一年」→ 表中「近一年」）。
 */
public final class IntervalLabelAligner {

    private static final Logger log = LoggerFactory.getLogger(IntervalLabelAligner.class);

    private IntervalLabelAligner() {}

    public static List<String> alignToMatrix(
            List<String> labels, List<List<String>> matrix, IntervalLexicon lexicon) {
        Set<String> matrixTexts = collectMatrixTexts(matrix);
        List<String> aligned = new ArrayList<>(labels.size());
        for (String label : labels) {
            String trimmed = label == null ? "" : label.trim();
            if (trimmed.isEmpty()) {
                aligned.add(trimmed);
                continue;
            }
            if (matrixTexts.contains(trimmed)) {
                aligned.add(trimmed);
                continue;
            }
            String kind = lexicon.resolveKind(trimmed);
            if (kind == null) {
                aligned.add(trimmed);
                continue;
            }
            String replacement =
                    matrixTexts.stream()
                            .filter(t -> kind.equals(lexicon.resolveKind(t)))
                            .findFirst()
                            .orElse(trimmed);
            if (!replacement.equals(trimmed)) {
                log.info(
                        "区间标签对齐: 「{}」→ 表中原文「{}」(kind={})",
                        trimmed,
                        replacement,
                        kind);
            }
            aligned.add(replacement);
        }
        return aligned;
    }

    private static Set<String> collectMatrixTexts(List<List<String>> matrix) {
        Set<String> out = new LinkedHashSet<>();
        for (List<String> row : matrix) {
            for (String cell : row) {
                if (cell != null) {
                    String t = cell.trim();
                    if (!t.isEmpty()) {
                        out.add(t);
                    }
                }
            }
        }
        return out;
    }
}
