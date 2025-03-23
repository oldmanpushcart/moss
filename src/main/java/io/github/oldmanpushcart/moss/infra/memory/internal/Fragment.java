package io.github.oldmanpushcart.moss.infra.memory.internal;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.MessageCodec;
import io.github.oldmanpushcart.moss.infra.memory.internal.domain.MemoryFragmentDO;

import java.time.Instant;

/**
 * 记忆体片段
 *
 * @param fragmentId      片段ID
 * @param uuid            UUID
 * @param tokens          TOKENS
 * @param requestMessage  请求消息
 * @param responseMessage 应答消息
 * @param createdAt       创建时间
 */
record Fragment(
        long fragmentId,
        String uuid,
        long tokens,
        Message requestMessage,
        Message responseMessage,
        Instant createdAt
) implements Comparable<Fragment> {

    /**
     * 从记忆片段数据转换为记忆片段
     *
     * @param fragmentDO 记忆片段数据
     * @return 记忆片段
     */
    public static Fragment fromMemoryFragmentDO(MemoryFragmentDO fragmentDO) {
        return new Fragment(
                fragmentDO.getFragmentId(),
                fragmentDO.getUuid(),
                fragmentDO.getTokens(),
                MessageCodec.decode(fragmentDO.getRequestMessageJson()),
                MessageCodec.decode(fragmentDO.getResponseMessageJson()),
                fragmentDO.getCreatedAt()
        );
    }

    @Override
    public int compareTo(Fragment o) {
        return Long.compare(this.fragmentId, o.fragmentId);
    }

}
