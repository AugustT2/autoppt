package com.example.pptrefresh.llm;

import com.example.pptrefresh.config.PptRefreshProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptCatalogTest {

    @Test
    void loadsV1PromptsFromClasspath() {
        PptRefreshProperties properties = new PptRefreshProperties();
        properties.setPromptsVersion("v1");
        PromptCatalog catalog = new PromptCatalog(properties, new DefaultResourceLoader());
        catalog.warmup();

        assertTrue(catalog.systemAgent().contains("你是 PPT 数据刷新助手"));
        assertTrue(catalog.systemAgent().contains("fetchNavChart"));
        assertTrue(catalog.systemStrict().contains("指定工具"));
        assertTrue(catalog.productNameSystem().contains("只输出一行"));
        assertTrue(catalog.tableQuerySystem().contains("intervalLabels"));
        assertFalse(catalog.systemAgent().startsWith("\n"));
    }

    @Test
    void failsWhenVersionDirectoryMissing() {
        PptRefreshProperties properties = new PptRefreshProperties();
        properties.setPromptsVersion("nonexistent-version");
        PromptCatalog catalog = new PromptCatalog(properties, new DefaultResourceLoader());
        assertThrows(IllegalStateException.class, catalog::warmup);
    }
}
