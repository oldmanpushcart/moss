package io.github.oldmanpushcart.moss.backend.chatter;

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
public interface Chatter {

    /**
     * 对话
     *
     * @param inputText 用户输入
     * @return 对话流应答
     */
    CompletionStage<Flowable<ChatResponse>> chat(Context context, String inputText);

    /**
     * 对话上下文
     */
    @Data
    @Accessors(chain = true)
    class Context {

        private boolean deepThinking;
        private List<File> attachments;
        private Long timeline;

    }

}
