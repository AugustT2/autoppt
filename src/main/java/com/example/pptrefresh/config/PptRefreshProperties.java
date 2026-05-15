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

    public static class Llm {
        private boolean enabled;
        private String baseUrl = "http://localhost:8081/v1";
        private String apiKey = "placeholder";
        private String modelName = "gpt-4o-mini";
        private int timeoutSeconds = 120;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
