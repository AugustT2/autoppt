package com.example.pptrefresh.llm;

import com.example.pptrefresh.config.PptRefreshProperties;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 从指定页纯文本推断一行基金/产品展示名（无 tools）。仅在规则策略 {@code LLM_EXTRACT} 且 llm.enabled 时使用。
 */
@Component
public class LlmProductNameExtractor {

    private static final int MAX_CHARS = 6000;

    private final PptRefreshProperties properties;
    private final PromptCatalog promptCatalog;
    private final ChatModel chatModel;

    public LlmProductNameExtractor(
            PptRefreshProperties properties,
            PromptCatalog promptCatalog,
            @Autowired(required = false) ChatModel chatModel) {
        this.properties = properties;
        this.promptCatalog = promptCatalog;
        this.chatModel = chatModel;
    }

    public String extractLine(String slidePlainText, String hint) {
        if (!properties.getLlm().isEnabled() || chatModel == null) {
            throw new RefreshException(
                    FailureStage.PRODUCT_NAME_RESOLVE,
                    "LLM_DISABLED",
                    "规则要求 LLM_EXTRACT 但未启用 ppt.refresh.llm.enabled",
                    null,
                    null);
        }
        String bodyText = slidePlainText;
        if (bodyText.length() > MAX_CHARS) {
            bodyText = bodyText.substring(0, MAX_CHARS);
        }
        String user =
                "下列文本来自 PPT 指定页面，请只输出一行：产品或基金的展示简称（不要代码、不要解释、不要引号）。\n"
                        + (hint != null && !hint.isBlank() ? "提示：" + hint + "\n\n" : "")
                        + bodyText;
        try {
            ChatResponse response =
                    chatModel.chat(
                            SystemMessage.from(promptCatalog.productNameSystem()),
                            UserMessage.from(user));
            String content = response.aiMessage().text();
            String line = content == null ? "" : content.trim().split("\\R", 2)[0].trim();
            if (line.isEmpty()) {
                throw new RefreshException(
                        FailureStage.PRODUCT_NAME_RESOLVE,
                        "LLM_EMPTY_NAME",
                        "LLM 未返回有效产品名",
                        null,
                        null);
            }
            return line;
        } catch (RefreshException e) {
            throw e;
        } catch (Exception e) {
            throw new RefreshException(
                    FailureStage.PRODUCT_NAME_RESOLVE,
                    "LLM_NAME_FAILED",
                    "产品名 LLM 调用失败: " + e.getMessage(),
                    null,
                    e);
        }
    }
}
