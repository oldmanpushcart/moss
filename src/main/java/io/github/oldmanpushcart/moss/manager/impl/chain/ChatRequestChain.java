package io.github.oldmanpushcart.moss.manager.impl.chain;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;

import java.util.concurrent.CompletionStage;

public interface ChatRequestChain {

    CompletionStage<ChatRequest> chain(ChatRequest request);

}
