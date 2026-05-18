package com.example.pptrefresh.rules;

import org.springframework.util.StringUtils;

/** 各 deck YAML 中 {@code productNameResolution.strategy} 的取值。 */
public enum ProductNameStrategy {
    /** 指定页含 anchorText 的文本框，对全文用正则第 1 捕获组作为展示名。 */
    ANCHOR_REGEX,
    /** 将若干页纯文本交给 LLM，只输出一行展示名（需开启 llm）。 */
    LLM_EXTRACT,
    /** 不解析展示名；基金代码亦为空（工具链按空名兜底）。 */
    EMPTY_OK,
    /** 使用规则中的字面量 literal，用于固定演示或无法从版式解析的 deck。 */
    STATIC;

    public static ProductNameStrategy fromYaml(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("productNameResolution.strategy 不能为空");
        }
        try {
            return ProductNameStrategy.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("未知 productNameResolution.strategy: " + raw, e);
        }
    }
}
