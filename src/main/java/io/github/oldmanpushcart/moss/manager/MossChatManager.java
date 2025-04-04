package io.github.oldmanpushcart.moss.manager;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.List;
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

    @Data
    @Accessors(chain = true)
    class Context {

        private List<File> attachments;
        private Long timeline;

    }

}
