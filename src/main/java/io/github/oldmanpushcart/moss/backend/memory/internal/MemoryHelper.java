package io.github.oldmanpushcart.moss.backend.memory.internal;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.LocalTokenizerUtils;
import io.github.oldmanpushcart.dashscope4j.util.MessageCodec;
import io.github.oldmanpushcart.moss.backend.memory.Memory;
import io.github.oldmanpushcart.moss.backend.memory.internal.domain.MemoryFragmentDO;

import static io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel.Mode.MULTIMODAL;

public class MemoryHelper {

    /**
     * 从记忆片段数据转换为记忆片段
     *
     * @param fragmentDO 记忆片段数据
     * @return 记忆片段
     */
    public static Memory.Fragment toMemoryFragment(MemoryFragmentDO fragmentDO) {
        return new Memory.Fragment()
                .setFragmentId(fragmentDO.getFragmentId())
                .setTokens(fragmentDO.getTokens())
                .setRequestMessage(MessageCodec.decode(fragmentDO.getRequestMessageJson()))
                .setResponseMessage(MessageCodec.decode(fragmentDO.getResponseMessageJson()))
                .setCreatedAt(fragmentDO.getCreatedAt())
                .setUpdatedAt(fragmentDO.getUpdatedAt());
    }


    /**
     * 从记忆片段转换为记忆片段数据
     *
     * @param fragment 记忆片段
     * @return 记忆片段数据
     */
    public static MemoryFragmentDO toMemoryFragmentDO(Memory.Fragment fragment) {

        final var requestTokens = computeMessageTokens(fragment.getRequestMessage());
        final var requestJson = MessageCodec.encodeToJson(MULTIMODAL, fragment.getRequestMessage());

        final var responseTokens = computeMessageTokens(fragment.getResponseMessage());
        final var responseJson = MessageCodec.encodeToJson(MULTIMODAL, fragment.getResponseMessage());

        return new MemoryFragmentDO()
                .setFragmentId(fragment.getFragmentId())
                .setTokens(requestTokens + responseTokens)
                .setRequestMessageJson(requestJson)
                .setResponseMessageJson(responseJson);

    }

    // 计算消息所消耗的TOKENS
    private static long computeMessageTokens(Message message) {
        return LocalTokenizerUtils.encode(message.text()).size();
    }

}
