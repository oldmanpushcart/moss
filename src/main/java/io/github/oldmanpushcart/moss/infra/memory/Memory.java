package io.github.oldmanpushcart.moss.infra.memory;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;

import java.util.List;

/**
 * 记忆体
 */
public interface Memory {

    /**
     * @return 回忆对话消息列表
     */
    List<Message> recall();

    /**
     * 添加对话消息
     * @param uuid            UUID
     * @param requestMessage  请求消息
     * @param responseMessage 应答消息
     */
    void append(String uuid, Message requestMessage, Message responseMessage);

}
