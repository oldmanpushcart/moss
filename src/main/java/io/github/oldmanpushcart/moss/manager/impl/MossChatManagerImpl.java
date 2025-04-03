package io.github.oldmanpushcart.moss.manager.impl;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.moss.manager.MossChatManager;
import io.github.oldmanpushcart.moss.manager.impl.interceptor.InjectSystemPromptInterceptor;
import io.github.oldmanpushcart.moss.manager.impl.interceptor.MemoryInterceptor;
import io.github.oldmanpushcart.moss.manager.impl.interceptor.RewriteUserMessageInterceptor;
import io.github.oldmanpushcart.moss.manager.impl.interceptor.RoutingToolsInterceptor;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Moss对话管理器实现
 */
@Slf4j
@Component
public class MossChatManagerImpl implements MossChatManager {

    private final ChatOpFlow chatOpFlow;

    @Autowired
    public MossChatManagerImpl(
            DashscopeClient dashscope,
            MemoryInterceptor memoryInterceptor,
            RoutingToolsInterceptor routingToolsInterceptor,
            InjectSystemPromptInterceptor injectSystemPromptInterceptor,
            RewriteUserMessageInterceptor rewriteUserMessageInterceptor
    ) {
        final var chatOp = dashscope.chat();
        this.chatOpFlow = InterceptionChatOpFlow.group(dashscope, chatOp::flow, List.of(
                memoryInterceptor,
                routingToolsInterceptor,
                injectSystemPromptInterceptor,
                rewriteUserMessageInterceptor
        ));
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> chat(ChatRequest request) {
        return chatOpFlow.flow(request)
                .whenComplete((v,ex)-> {
                    if(null != ex) {
                        log.warn("moss://chat/flow error!", ex);
                    }
                });
    }

}
