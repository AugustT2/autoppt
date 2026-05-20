package com.example.pptrefresh.config;

import com.example.pptrefresh.llm.LangChain4jLlmTaskRunner;
import com.example.pptrefresh.llm.LlmTaskRunner;
import com.example.pptrefresh.llm.PromptBuilder;
import com.example.pptrefresh.llm.WritePayloadParser;
import com.example.pptrefresh.write.TaskWritePayloadEnricher;
import com.example.pptrefresh.tools.DemoDataTools;
import com.example.pptrefresh.tools.DemoToolExecutor;
import com.example.pptrefresh.tools.ToolCatalog;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM：由 {@code langchain4j-open-ai-spring-boot-starter} 注册 {@link ChatModel}，
 * 任务取数统一走 {@link LangChain4jLlmTaskRunner}（需 {@code ppt.refresh.llm.enabled=true}）。
 */
@Configuration
@EnableConfigurationProperties(PptRefreshProperties.class)
public class PptRefreshConfiguration {

    @Bean
    DemoToolExecutor demoToolExecutor(DemoDataTools demoDataTools) {
        return new DemoToolExecutor(demoDataTools);
    }

    @Bean
    LlmTaskRunner llmTaskRunner(
            PptRefreshProperties properties,
            WritePayloadParser writePayloadParser,
            DemoToolExecutor demoToolExecutor,
            ToolCatalog toolCatalog,
            PromptBuilder promptBuilder,
            TaskWritePayloadEnricher payloadEnricher,
            @Autowired(required = false) ChatModel chatModel) {
        if (!properties.getLlm().isEnabled()) {
            throw new IllegalStateException(
                    "ppt.refresh.llm.enabled 必须为 true（已移除无 LLM 的 Stub 取数路径）");
        }
        if (chatModel == null) {
            throw new IllegalStateException(
                    "未创建 ChatModel：请配置 langchain4j.open-ai.chat-model.api-key（如环境变量 DASHSCOPE_API_KEY）");
        }
        return new LangChain4jLlmTaskRunner(
                chatModel,
                writePayloadParser,
                demoToolExecutor,
                toolCatalog,
                promptBuilder,
                payloadEnricher);
    }
}
