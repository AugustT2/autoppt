package com.example.pptrefresh.query;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.rules.TaskDefinition;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 表格刷新：Java 读矩阵 → LLM 识别区间+指标 → Java 词表+规则解析条件 → 组装 {@link TableAnalysis}。
 */
@Service
public class TableQueryInferenceService {

    private static final Logger log = LoggerFactory.getLogger(TableQueryInferenceService.class);

    private final LlmTableQueryExtractor llmExtractor;

    public TableQueryInferenceService(LlmTableQueryExtractor llmExtractor) {
        this.llmExtractor = llmExtractor;
    }

    public TableAnalysis analyze(TaskDefinition task, XSLFTable table, IntervalLexicon lexicon) {
        List<List<String>> matrix = TableMatrixReader.read(table);
        TableAnalysis analysis;
        if (llmExtractor.isAvailable()) {
            try {
                TableQueryIntent intent =
                        llmExtractor.infer(matrix, task.getIntent(), lexicon);
                analysis = TableMatrixLayoutResolver.resolve(matrix, intent, "llm");
                validate(analysis, matrix, lexicon, task.getId());
                log.info(
                        "表格查询分析 task={} source=llm intervals={} metrics={} axis={}",
                        task.getId(),
                        analysis.intervalLabels(),
                        analysis.metrics(),
                        analysis.intervalAxis());
                return analysis;
            } catch (Exception e) {
                log.warn("表格 LLM 意图识别失败 task={}，回退启发式: {}", task.getId(), e.getMessage());
            }
        }
        analysis = analyzeHeuristic(table, matrix, lexicon);
        validate(analysis, matrix, lexicon, task.getId());
        log.info(
                "表格查询分析 task={} source=heuristic intervals={} metrics={}",
                task.getId(),
                analysis.intervalLabels().size(),
                analysis.metrics());
        return analysis;
    }

    private static TableAnalysis analyzeHeuristic(
            XSLFTable table, List<List<String>> matrix, IntervalLexicon lexicon) {
        TableIntervalDimensionExtractor.TableLabelScanResult scan =
                TableIntervalDimensionExtractor.scan(
                        table, TableLabelAxis.AUTO, 0, 1, lexicon);
        if (scan.labels().isEmpty()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "INTERVAL_LABELS_EMPTY",
                    "启发式未识别到区间标签",
                    null,
                    null);
        }
        TableQueryIntent intent = TableMatrixLayoutResolver.intentFromScan(scan);
        return TableMatrixLayoutResolver.resolveFromScan(scan, intent, "heuristic");
    }

    private static void validate(
            TableAnalysis analysis,
            List<List<String>> matrix,
            IntervalLexicon lexicon,
            String taskId) {
        if (analysis.intervalLabels().isEmpty()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "INTERVAL_LABELS_EMPTY",
                    "区间标签为空",
                    taskId,
                    null);
        }
        if (analysis.metrics().isEmpty()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "TABLE_METRICS_EMPTY",
                    "指标列为空",
                    taskId,
                    null);
        }
        for (String label : analysis.intervalLabels()) {
            if (lexicon.resolveKind(label) == null) {
                throw new RefreshException(
                        FailureStage.CONDITION_RESOLVE,
                        "LEXICON_UNKNOWN_LABEL",
                        "词表无法识别区间标签: " + label,
                        taskId,
                        null);
            }
            if (!labelInMatrix(analysis, matrix, label)) {
                throw new RefreshException(
                        FailureStage.DIMENSION_EXTRACT,
                        "INTERVAL_LABEL_NOT_IN_MATRIX",
                        "区间标签不在表格中: " + label,
                        taskId,
                        null);
            }
        }
    }

    private static boolean labelInMatrix(
            TableAnalysis analysis, List<List<String>> matrix, String label) {
        String target = label.trim();
        if (analysis.intervalAxis() == TableLabelAxis.ROW) {
            int col = analysis.intervalLabelIndex();
            for (List<String> row : matrix) {
                if (col < row.size() && target.equals(row.get(col).trim())) {
                    return true;
                }
            }
            return false;
        }
        int rowIdx = analysis.intervalLabelIndex();
        if (rowIdx >= matrix.size()) {
            return false;
        }
        for (String cell : matrix.get(rowIdx)) {
            if (target.equals(cell.trim())) {
                return true;
            }
        }
        return false;
    }
}
