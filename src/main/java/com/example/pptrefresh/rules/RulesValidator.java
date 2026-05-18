package com.example.pptrefresh.rules;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Component
public class RulesValidator {

    public void validate(DeckRules rules) {
        if (!StringUtils.hasText(rules.getDeckType())) {
            throw schemaError("deckType 不能为空");
        }
        if (rules.getTasks() == null || rules.getTasks().isEmpty()) {
            throw schemaError("tasks 不能为空");
        }
        Set<String> ids = new HashSet<>();
        for (TaskDefinition task : rules.getTasks()) {
            if (!StringUtils.hasText(task.getId())) {
                throw schemaError("task.id 不能为空");
            }
            if (!ids.add(task.getId())) {
                throw schemaError("task.id 重复: " + task.getId());
            }
            if (task.getType() == null) {
                throw schemaError("task.type 不能为空: " + task.getId());
            }
            if (!StringUtils.hasText(task.getIntent())) {
                throw schemaError("task.intent 不能为空: " + task.getId());
            }
            switch (task.getType()) {
                case text:
                    validateText(task);
                    break;
                case table:
                    validateTable(task);
                    break;
                case chart:
                    validateChart(task);
                    break;
                default:
                    throw schemaError("未知 task.type: " + task.getId());
            }
        }
        validateProductNameResolution(rules.getProductNameResolution());
    }

    private void validateProductNameResolution(ProductNameResolution cfg) {
        if (cfg == null) {
            throw schemaError("productNameResolution 不能为空");
        }
        if (!StringUtils.hasText(cfg.getStrategy())) {
            throw schemaError("productNameResolution.strategy 不能为空");
        }
        ProductNameStrategy st;
        try {
            st = cfg.strategyEnum();
        } catch (IllegalArgumentException e) {
            throw schemaError(e.getMessage());
        }
        switch (st) {
            case ANCHOR_REGEX:
                if (!StringUtils.hasText(cfg.getAnchorText())) {
                    throw schemaError("ANCHOR_REGEX 需要 anchorText");
                }
                if (!StringUtils.hasText(cfg.getPattern())) {
                    throw schemaError("ANCHOR_REGEX 需要 pattern");
                }
                break;
            case LLM_EXTRACT:
                break;
            case EMPTY_OK:
                break;
            case STATIC:
                if (!StringUtils.hasText(cfg.getLiteral())) {
                    throw schemaError("STATIC 需要 literal");
                }
                break;
            default:
                throw schemaError("未知 productNameResolution.strategy");
        }
    }

    private void validateText(TaskDefinition task) {
        if (!StringUtils.hasText(task.getAnchorText())) {
            throw schemaError("text 任务需要 anchorText: " + task.getId());
        }
        if (task.getMode() == null) {
            throw schemaError("text 任务需要 mode: " + task.getId());
        }
    }

    private void validateTable(TaskDefinition task) {
        if (task.getTableOrdinal() == null || task.getTableOrdinal() < 1) {
            throw schemaError("table 任务需要 tableOrdinal >= 1: " + task.getId());
        }
    }

    private void validateChart(TaskDefinition task) {
        if (task.getChartOrdinal() == null || task.getChartOrdinal() < 1) {
            throw schemaError("chart 任务需要 chartOrdinal >= 1: " + task.getId());
        }
    }

    private RefreshException schemaError(String message) {
        return new RefreshException(FailureStage.RULES_SCHEMA, "RULES_SCHEMA", message);
    }
}
