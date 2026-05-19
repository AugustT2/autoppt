package com.example.pptrefresh.llm;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.tools.DemoToolExecutor;
import com.example.pptrefresh.tools.ToolCatalog;
import com.example.pptrefresh.write.TaskWritePayload;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用 LangChain4j {@link ChatModel} 完成 task 级对话与 Tool 多轮循环。
 * <ul>
 *   <li>Agent 模式（默认）：YAML 未配置 {@code tool} → 挂全部 Tool，由 SYSTEM 引导选型</li>
 *   <li>流水线模式：YAML 配置 {@code tool} → 只挂该 Tool</li>
 * </ul>
 */
public class LangChain4jLlmTaskRunner implements LlmTaskRunner {

    private static final int MAX_TOOL_ROUNDS = 8;

    private final ChatModel chatModel;
    private final WritePayloadParser parser;
    private final DemoToolExecutor toolExecutor;
    private final ToolCatalog toolCatalog;
    private final PromptBuilder promptBuilder = new PromptBuilder();

    public LangChain4jLlmTaskRunner(
            ChatModel chatModel,
            WritePayloadParser parser,
            DemoToolExecutor toolExecutor,
            ToolCatalog toolCatalog) {
        this.chatModel = chatModel;
        this.parser = parser;
        this.toolExecutor = toolExecutor;
        this.toolCatalog = toolCatalog;
    }

    @Override
    public TaskWritePayload fetch(TaskContext context) {
        String configuredTool = context.task().getTool();
        boolean strict = !toolCatalog.isAgentMode(configuredTool);
        List<ToolSpecification> taskTools = toolCatalog.resolveForTask(configuredTool);
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(promptBuilder.systemMessage(context.task())));
            messages.add(
                    UserMessage.from(
                            promptBuilder.buildUserMessage(
                                    context.deckType(),
                                    context.productDisplayName(),
                                    context.fundCode(),
                                    context.timeContext(),
                                    context.task(),
                                    context.structure())));

            ChatRequest.Builder requestBuilder =
                    ChatRequest.builder().messages(messages).toolSpecifications(taskTools);

            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                ChatResponse response = chatModel.chat(requestBuilder.build());
                AiMessage ai = response.aiMessage();
                messages.add(ai);

                if (!ai.hasToolExecutionRequests()) {
                    String text = ai.text();
                    if (text != null && !text.isBlank()) {
                        return parser.parse(context.task(), text);
                    }
                    throw new RefreshException(
                            FailureStage.TASK_LLM,
                            "LLM_EMPTY_CONTENT",
                            "模型未返回可解析的文本",
                            context.task().getId(),
                            null);
                }

                for (ToolExecutionRequest toolRequest : ai.toolExecutionRequests()) {
                    if (strict && !configuredTool.trim().equals(toolRequest.name())) {
                        throw new RefreshException(
                                FailureStage.TASK_LLM,
                                "TOOL_NOT_ALLOWED",
                                "本任务仅允许 tool="
                                        + configuredTool
                                        + "，模型请求了 "
                                        + toolRequest.name(),
                                context.task().getId(),
                                null);
                    }
                    String result = toolExecutor.execute(toolRequest.name(), toolRequest.arguments());
                    messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                }
                requestBuilder =
                        ChatRequest.builder().messages(messages).toolSpecifications(taskTools);
            }
            throw new RefreshException(
                    FailureStage.TASK_LLM,
                    "TOOL_LOOP_LIMIT",
                    "Tool 调用轮次超过上限",
                    context.task().getId(),
                    null);
        } catch (RefreshException e) {
            throw e;
        } catch (Exception e) {
            throw new RefreshException(
                    FailureStage.TASK_LLM,
                    "LLM_FAILED",
                    "LangChain4j 调用失败: " + e.getMessage(),
                    context.task().getId(),
                    e);
        }
    }
}
