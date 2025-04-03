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
