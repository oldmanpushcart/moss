package io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

public interface ChatInterceptor {

    /**
     * 拦截操作
     *
     * @param chain 操作链
     * @return 操作结果
     */
    CompletionStage<Flowable<ChatResponse>> intercept(Chain chain);

    /**
     * 操作链
     */
    interface Chain {

        /**
         * @return Dashscope 客户端
         */
        DashscopeClient dashscope();

        /**
         * @return 操作请求
         */
        ChatRequest request();

        /**
         * 执行操作
         *
         * @param request 操作请求
         * @return 操作结果
         */
        CompletionStage<Flowable<ChatResponse>> process(ChatRequest request);

    }

}
