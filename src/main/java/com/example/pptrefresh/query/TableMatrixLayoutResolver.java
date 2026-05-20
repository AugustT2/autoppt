package com.example.pptrefresh.query;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 根据矩阵与区间/指标语义确定表头与区间轴：在矩阵中定位 intervalLabels，同行→COLUMN、同列→ROW。
 * LLM 的 intervalAxis 仅作可选校验，不一致时 warn 并以矩阵为准。
 */
public final class TableMatrixLayoutResolver {

    private static final Logger log = LoggerFactory.getLogger(TableMatrixLayoutResolver.class);

    private TableMatrixLayoutResolver() {}

    public static TableAnalysis resolve(
            List<List<String>> matrix, TableQueryIntent intent, String source) {
        AxisDetection det = resolveAxisFromLabelPositions(matrix, intent.intervalLabels());
        intent.intervalAxis()
                .filter(llm -> llm != det.axis())
                .ifPresent(
                        llm ->
                                log.warn(
                                        "LLM intervalAxis={} 与矩阵推断 {} 不一致，以矩阵为准",
                                        llm,
                                        det.axis()));
        List<String> columnHeaders = headerRow(matrix, det.axis(), det.labelIndex());
        List<String> rowHeaders =
                det.axis() == TableLabelAxis.COLUMN ? new ArrayList<>(intent.metrics()) : List.of();
        return new TableAnalysis(
                intent.intervalLabels(),
                intent.metrics(),
                columnHeaders,
                rowHeaders,
                det.axis(),
                det.labelIndex(),
                source);
    }

    static List<String> headerRow(
            List<List<String>> matrix, TableLabelAxis axis, int labelIndex) {
        if (matrix.isEmpty()) {
            return List.of();
        }
        if (axis == TableLabelAxis.COLUMN) {
            return labelIndex < matrix.size() ? new ArrayList<>(matrix.get(labelIndex)) : List.of();
        }
        return new ArrayList<>(matrix.get(0));
    }

    /**
     * 在矩阵中定位每个区间标签，全部同行→{@link TableLabelAxis#COLUMN}，全部同列→{@link TableLabelAxis#ROW}。
     */
    static AxisDetection resolveAxisFromLabelPositions(
            List<List<String>> matrix, List<String> intervalLabels) {
        if (intervalLabels == null || intervalLabels.isEmpty()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "INTERVAL_LABELS_EMPTY",
                    "区间标签为空",
                    null,
                    null);
        }
        List<CellPos> positions = new ArrayList<>(intervalLabels.size());
        for (String label : intervalLabels) {
            positions.add(
                    findFirst(matrix, label)
                            .orElseThrow(
                                    () ->
                                            new RefreshException(
                                                    FailureStage.DIMENSION_EXTRACT,
                                                    "INTERVAL_LABEL_NOT_IN_MATRIX",
                                                    "区间标签不在表格中: " + label,
                                                    null,
                                                    null)));
        }
        Set<Integer> rows = new HashSet<>();
        Set<Integer> cols = new HashSet<>();
        for (CellPos p : positions) {
            rows.add(p.row());
            cols.add(p.col());
        }
        if (intervalLabels.size() == 1) {
            CellPos p = positions.get(0);
            if (p.col() == 0 && p.row() > 0) {
                return new AxisDetection(TableLabelAxis.ROW, 0);
            }
            if (p.row() == 0 && p.col() > 0) {
                return new AxisDetection(TableLabelAxis.COLUMN, p.row());
            }
        } else {
            if (rows.size() == 1) {
                return new AxisDetection(TableLabelAxis.COLUMN, rows.iterator().next());
            }
            if (cols.size() == 1) {
                return new AxisDetection(TableLabelAxis.ROW, cols.iterator().next());
            }
        }
        throw new RefreshException(
                FailureStage.DIMENSION_EXTRACT,
                "INTERVAL_LAYOUT_MISMATCH",
                "区间标签未落在同一行或同一列，无法确定 intervalAxis",
                null,
                null);
    }

    private static Optional<CellPos> findFirst(List<List<String>> matrix, String label) {
        String target = label.trim();
        for (int r = 0; r < matrix.size(); r++) {
            List<String> row = matrix.get(r);
            for (int c = 0; c < row.size(); c++) {
                if (target.equals(row.get(c).trim())) {
                    return Optional.of(new CellPos(r, c));
                }
            }
        }
        return Optional.empty();
    }

    private record CellPos(int row, int col) {}

    private static record AxisDetection(TableLabelAxis axis, int labelIndex) {}
}
