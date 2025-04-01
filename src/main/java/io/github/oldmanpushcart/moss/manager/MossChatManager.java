package io.github.oldmanpushcart.moss.manager;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

public interface MossChatManager {

    CompletionStage<Flowable<ChatResponse>> chat(MossChatContext context);

}
