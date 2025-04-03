package io.github.oldmanpushcart.moss.manager;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

/**
 * Moss对话管理器
 */
public interface MossChatManager {

    /**
     * 对话
     *
     * @param request 请求
     * @return 对话流应答
     */
    CompletionStage<Flowable<ChatResponse>> chat(ChatRequest request);

}
