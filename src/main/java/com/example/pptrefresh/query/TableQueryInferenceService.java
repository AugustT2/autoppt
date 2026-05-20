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
        try {
            TableQueryIntent intent =
                    llmExtractor.infer(matrix, task.getIntent(), lexicon);
            TableAnalysis analysis = TableMatrixLayoutResolver.resolve(matrix, intent, "llm");
            validate(analysis, matrix, lexicon, task.getId());
            log.info(
                    "表格查询分析 task={} intervals={} metrics={} axis={}",
                    task.getId(),
                    analysis.intervalLabels().size(),
                    analysis.metrics(),
                    analysis.intervalAxis());
            return analysis;
        } catch (RefreshException e) {
            throw e;
        } catch (Exception e) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "TABLE_QUERY_INFERENCE_FAILED",
                    "表格查询意图识别失败: " + e.getMessage(),
                    task.getId(),
                    e);
        }
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
