package io.github.oldmanpushcart.moss.backend.chatter.internal;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor.MemoryChatInterceptor;
import io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor.RewriteUserMessageChatInterceptor;
import io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor.RoutingToolsChatInterceptor;
import io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor.SystemPromptChatInterceptor;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 对话管理器实现
 */
@Slf4j
@Component
public class ChatterImpl implements Chatter {

    private final ChatOpFlow chatOpFlow;

    @Autowired
    public ChatterImpl(
            DashscopeClient dashscope,
            MemoryChatInterceptor memoryChatInterceptor,
            RoutingToolsChatInterceptor routingToolsChatInterceptor,
            SystemPromptChatInterceptor systemPromptChatInterceptor,
            RewriteUserMessageChatInterceptor rewriteUserMessageChatInterceptor
    ) {
        final var chatOp = dashscope.chat();
        this.chatOpFlow = InterceptionChatOpFlow.group(dashscope, chatOp::flow, List.of(
                memoryChatInterceptor,
                routingToolsChatInterceptor,
                systemPromptChatInterceptor,
                rewriteUserMessageChatInterceptor
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
