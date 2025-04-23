package io.github.oldmanpushcart.moss.backend.chatter.internal;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatSearchOption.SearchStrategy;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor.*;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * 对话管理器实现
 */
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class ChatterImpl implements Chatter {

    private final DashscopeClient dashscope;
    private final MemoryInterceptor memoryInterceptor;
    private final RoutingToolsInterceptor routingToolsInterceptor;
    private final SystemPromptInterceptor systemPromptInterceptor;
    private final RewriteUserMessageInterceptor rewriteUserMessageInterceptor;
    private final KnowledgeInterceptor knowledgeInterceptor;

    @Override
    public CompletionStage<Flowable<ChatResponse>> chat(Context context, String inputText) {
        final var request = newChatRequest(context, inputText);
        return dashscope.chat().flow(request)
                .thenApply(responseFlow ->
                        responseFlow.doOnError(ex -> log.warn("moss://chat/flow error!", ex)))
                .whenComplete((v, ex) -> {
                    if (null != ex) {
                        log.warn("moss://chat error!", ex);
                    }
                });
    }

    // 构建对话请求
    private ChatRequest newChatRequest(Context context, String inputText) {
        return ChatRequest.newBuilder()
                .context(Chatter.Context.class, context)
                .model(decideChatModel(context))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .option(ChatOptions.ENABLE_WEB_SEARCH, context.isWebSearchEnabled())
                .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption()
                        .forcedSearch(false)
                        .enableSource(true)
                        .searchStrategy(SearchStrategy.STANDARD))
                .addInterceptors(List.of(
                        memoryInterceptor,
                        knowledgeInterceptor,
                        rewriteUserMessageInterceptor,
                        routingToolsInterceptor,
                        systemPromptInterceptor
                ))
                .addMessage(Message.ofUser(inputText))
                .build();
    }

    // 决定采用那个对话模型
    private ChatModel decideChatModel(Context context) {
        return context.isDeepThinkingEnabled()
                ? ChatModel.QWQ_PLUS
                : ChatModel.QWEN_MAX;
    }

}
