package io.github.oldmanpushcart.moss.backend.dashscope.util;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;

import java.util.List;

/**
 * Dashscope 工具类
 */
public class DashscopeUtils {

    /**
     * 获取最后一个消息
     *
     * @param request 对话请求
     * @return 最后一个消息
     * @throws IllegalArgumentException 如果没有消息则抛出此异常
     */
    public static Message requireLastMessage(ChatRequest request) {
        final List<Message> messages = request.messages();
        if (null == messages || messages.isEmpty()) {
            throw new IllegalArgumentException("Last message not existed!");
        }
        return messages.get(messages.size() - 1);
    }

    /**
     * 获取最后一个用户消息
     *
     * @param request 对话请求
     * @return 最后一个用户消息
     * @throws IllegalArgumentException 如果没有消息或最后一个消息不是USER消息则抛出此异常
     */
    public static Message requireLastUserMessage(ChatRequest request) {
        final Message lastMessage = requireLastMessage(request);
        if (lastMessage.role() != Message.Role.USER) {
            throw new IllegalStateException("Last message is not user message!");
        }
        return lastMessage;
    }

}
