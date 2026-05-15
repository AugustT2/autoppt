package com.example.pptrefresh.config;

import com.example.pptrefresh.funds.HardcodedFundCodeLookup;
import com.example.pptrefresh.llm.HttpOpenAiLlmTaskRunner;
import com.example.pptrefresh.llm.LlmTaskRunner;
import com.example.pptrefresh.llm.StubLlmTaskRunner;
import com.example.pptrefresh.llm.WritePayloadParser;
import com.example.pptrefresh.tools.DemoDataTools;
import com.example.pptrefresh.tools.DemoToolExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(PptRefreshProperties.class)
public class PptRefreshConfiguration {

    @Bean
    DemoDataTools demoDataTools(HardcodedFundCodeLookup fundCodeLookup) {
        return new DemoDataTools(fundCodeLookup);
    }

    @Bean
    DemoToolExecutor demoToolExecutor(DemoDataTools demoDataTools) {
        return new DemoToolExecutor(demoDataTools);
    }

    @Bean
    RestTemplate llmRestTemplate(RestTemplateBuilder builder, PptRefreshProperties properties) {
        return builder
                .setConnectTimeout(
                        java.time.Duration.ofSeconds(properties.getLlm().getTimeoutSeconds()))
                .setReadTimeout(java.time.Duration.ofSeconds(properties.getLlm().getTimeoutSeconds()))
                .build();
    }

    @Bean
    LlmTaskRunner llmTaskRunner(
            PptRefreshProperties properties,
            WritePayloadParser writePayloadParser,
            DemoToolExecutor demoToolExecutor,
            RestTemplate llmRestTemplate) {
        if (properties.getLlm().isEnabled()) {
            return new HttpOpenAiLlmTaskRunner(
                    properties, writePayloadParser, demoToolExecutor, llmRestTemplate);
        }
        return new StubLlmTaskRunner();
    }
}
