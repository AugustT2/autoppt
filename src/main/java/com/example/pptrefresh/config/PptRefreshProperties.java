package com.example.pptrefresh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ppt.refresh")
public class PptRefreshProperties {

    private String rulesDir = "classpath:rules";
    private int slideIndexBase = 0;
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
