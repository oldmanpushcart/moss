package io.github.oldmanpushcart.moss.manager.impl;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.moss.manager.impl.interceptor.MossChatInterceptor;
import io.reactivex.rxjava3.core.Flowable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 拦截式对话流
 *
 * @param dashscope   通义千问客户端
 * @param opFlow      对话流
 * @param interceptor 拦截器
 */
record InterceptionChatOpFlow(
        DashscopeClient dashscope,
        ChatOpFlow opFlow,
        MossChatInterceptor interceptor
) implements ChatOpFlow {

    private record ChainImpl(
            DashscopeClient dashscope,
            ChatRequest request,
            Function<ChatRequest, CompletionStage<Flowable<ChatResponse>>> applier
    ) implements MossChatInterceptor.Chain {

        @Override
        public CompletionStage<Flowable<ChatResponse>> process(ChatRequest request) {
            return applier.apply(request);
        }

    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        final MossChatInterceptor.Chain chain = new ChainImpl(dashscope, request, opFlow::flow);
        return interceptor.intercept(chain);
    }

    /**
     * 组合拦截器
     *
     * @param dashscope    通义千问客户端
     * @param opFlow       对话流
     * @param interceptors 拦截器集合
     * @return 组合拦截器
     */
    public static ChatOpFlow group(DashscopeClient dashscope, ChatOpFlow opFlow, List<MossChatInterceptor> interceptors) {
        final var cloneInterceptors = new ArrayList<>(interceptors);
        Collections.reverse(cloneInterceptors);
        ChatOpFlow op = opFlow;
        for (final MossChatInterceptor interceptor : cloneInterceptors) {
            op = new InterceptionChatOpFlow(dashscope, op, interceptor);
        }
        return op;
    }

}
