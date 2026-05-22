package com.example.pptrefresh.llm;

import com.example.pptrefresh.config.PptRefreshProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从 {@code classpath:prompts/{version}/*.md} 加载 LLM System 等固定提示词。
 */
@Component
public class PromptCatalog {

    static final String SYSTEM_AGENT = "system-agent";
    static final String SYSTEM_STRICT = "system-strict";
    static final String PRODUCT_NAME_SYSTEM = "product-name-system";
    static final String TABLE_QUERY_SYSTEM = "table-query-system";

    private static final List<String> REQUIRED =
            List.of(SYSTEM_AGENT, SYSTEM_STRICT, PRODUCT_NAME_SYSTEM, TABLE_QUERY_SYSTEM);

    private final String promptsVersion;
    private final ResourceLoader resourceLoader;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public PromptCatalog(PptRefreshProperties properties, ResourceLoader resourceLoader) {
        this.promptsVersion = properties.getPromptsVersion();
        this.resourceLoader = resourceLoader;
    }

    public String version() {
        return promptsVersion;
    }

    public String systemAgent() {
        return require(SYSTEM_AGENT);
    }

    public String systemStrict() {
        return require(SYSTEM_STRICT);
    }

    public String productNameSystem() {
        return require(PRODUCT_NAME_SYSTEM);
    }

    public String tableQuerySystem() {
        return require(TABLE_QUERY_SYSTEM);
    }

    @PostConstruct
    void warmup() {
        for (String name : REQUIRED) {
            require(name);
        }
    }

    private String require(String name) {
        return cache.computeIfAbsent(name, this::load);
    }

    private String load(String name) {
        String location = "classpath:prompts/" + promptsVersion + "/" + name + ".md";
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("缺少 prompt 资源: " + location);
        }
        try {
            String raw = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return raw.trim();
        } catch (IOException e) {
            throw new IllegalStateException("读取 prompt 失败: " + location, e);
        }
    }
}
