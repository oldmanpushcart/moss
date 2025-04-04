package io.github.oldmanpushcart.moss.backend.chatter.internal;

import io.github.oldmanpushcart.dashscope4j.OpFlow;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;

/**
 * 对话操作流
 */
public interface ChatOpFlow extends OpFlow<ChatRequest, ChatResponse> {
}
