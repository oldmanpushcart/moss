package io.github.oldmanpushcart.moss.util;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

/**
 * Dashscope 工具类
 */
public class DashscopeUtils {

    /**
     * 判断最后一个消息是否是用户消息
     *
     * @param request 对话请求
     * @return TRUE | FALSE
     */
    public static boolean isLastMessageFromUser(ChatRequest request) {
        final var messages = request.messages();
        if (null == messages || messages.isEmpty()) {
            return false;
        }
        final var lastMessage = messages.get(messages.size() - 1);
        return null != lastMessage
               && lastMessage.role() == Message.Role.USER;
    }

    /**
     * 获取最后一个用户消息
     *
     * @param request 对话请求
     * @return 最后一个用户消息
     * @throws IllegalArgumentException 如果没有消息或最后一个消息不是USER消息则抛出此异常
     */
    public static Message requireLastMessageFromUser(ChatRequest request) {
        final var messages = request.messages();
        if (null == messages || messages.isEmpty()) {
            throw new IllegalArgumentException("Last message not existed!");
        }
        final var lastMessage = messages.get(messages.size() - 1);
        if (null == lastMessage
            || lastMessage.role() != Message.Role.USER) {
            throw new IllegalArgumentException("Last message not user message!");
        }
        return lastMessage;
    }

    /**
     * 提取历史信息
     * <p>
     * 消息列表中下标范围[0,n-1)信息为历史信息
     * </p>
     *
     * @param request 对话请求
     * @return 历史信息
     */
    public static List<Message> requireHistoryMessages(ChatRequest request) {
        final var messages = request.messages();
        return null == messages || messages.isEmpty()
                ? emptyList()
                : request.messages().subList(0, request.messages().size() - 1);
    }

    /**
     * 是否来自MOSS对话管理器发起的请求
     *
     * @param request 对话请求
     * @return TRUE | FALSE
     */
    public static boolean isCameFromChatter(ChatRequest request) {
        return Objects.nonNull(request.context(Chatter.Context.class));
    }

}
