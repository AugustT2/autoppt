package com.example.pptrefresh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ppt.refresh")
public class PptRefreshProperties {

    private String rulesDir = "classpath:rules";
    private int slideIndexBase = 0;
    /** 图表写回：cache-only（默认，不重写嵌入 xlsx）| embedded-workbook */
    private String chartWriteMode = "cache-only";
    private final Llm llm = new Llm();

    public String getRulesDir() {
        return rulesDir;
    }

    public void setRulesDir(String rulesDir) {
        this.rulesDir = rulesDir;
    }

    public int getSlideIndexBase() {
        return slideIndexBase;
    }

    public void setSlideIndexBase(int slideIndexBase) {
        this.slideIndexBase = slideIndexBase;
    }

    public String getChartWriteMode() {
        return chartWriteMode;
    }

    public void setChartWriteMode(String chartWriteMode) {
        this.chartWriteMode = chartWriteMode;
    }

    public com.example.pptrefresh.document.ChartWriteMode chartWriteModeEnum() {
        if (chartWriteMode == null || chartWriteMode.isBlank() || "cache-only".equalsIgnoreCase(chartWriteMode)) {
            return com.example.pptrefresh.document.ChartWriteMode.CACHE_ONLY;
        }
        if ("embedded-workbook".equalsIgnoreCase(chartWriteMode)) {
            return com.example.pptrefresh.document.ChartWriteMode.EMBEDDED_WORKBOOK;
        }
        throw new IllegalArgumentException("未知 ppt.refresh.chart-write-mode: " + chartWriteMode);
    }

    public Llm getLlm() {
        return llm;
    }

    /** 是否调用 LangChain4j {@link dev.langchain4j.model.chat.ChatModel}；模型参数见 {@code langchain4j.open-ai.*}。 */
    public static class Llm {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
