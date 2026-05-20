package com.example.pptrefresh.config;

import com.example.pptrefresh.funds.FundFactsClient;
import com.example.pptrefresh.query.QueryPlanDataService;
import com.example.pptrefresh.llm.LangChain4jLlmTaskRunner;
import com.example.pptrefresh.llm.LlmTaskRunner;
import com.example.pptrefresh.llm.PromptBuilder;
import com.example.pptrefresh.llm.StubLlmTaskRunner;
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
 * LLM：由 {@code langchain4j-open-ai-spring-boot-starter} 根据 {@code langchain4j.open-ai.*} 自动注册
 * {@link ChatModel}；本类只负责在 {@code ppt.refresh.llm.enabled} 下选择 Stub 或 LangChain4j 实现。
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
            FundFactsClient fundFactsClient,
            QueryPlanDataService queryPlanDataService,
            @Autowired(required = false) ChatModel chatModel) {
        if (properties.getLlm().isEnabled()) {
            if (chatModel == null) {
                throw new IllegalStateException(
                        "ppt.refresh.llm.enabled=true 但未创建 ChatModel：请配置 langchain4j.open-ai.chat-model.api-key");
            }
            return new LangChain4jLlmTaskRunner(
                    chatModel,
                    writePayloadParser,
                    demoToolExecutor,
                    toolCatalog,
                    promptBuilder,
                    payloadEnricher);
        }
        return new StubLlmTaskRunner(fundFactsClient, queryPlanDataService);
    }
}
