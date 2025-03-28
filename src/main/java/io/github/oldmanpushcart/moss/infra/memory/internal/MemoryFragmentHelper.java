package io.github.oldmanpushcart.moss.infra.memory.internal;

import io.github.oldmanpushcart.dashscope4j.util.MessageCodec;
import io.github.oldmanpushcart.moss.infra.memory.MemoryFragment;
import io.github.oldmanpushcart.moss.infra.memory.internal.domain.MemoryFragmentDO;

public class MemoryFragmentHelper {

    /**
     * 从记忆片段数据转换为记忆片段
     *
     * @param fragmentDO 记忆片段数据
     * @return 记忆片段
     */
    public static MemoryFragment fromMemoryFragmentDO(MemoryFragmentDO fragmentDO) {
        return new MemoryFragment()
                .fragmentId(fragmentDO.getFragmentId())
                .tokens(fragmentDO.getTokens())
                .requestMessage(MessageCodec.decode(fragmentDO.getRequestMessageJson()))
                .responseMessage(MessageCodec.decode(fragmentDO.getResponseMessageJson()))
                .createdAt(fragmentDO.getCreatedAt())
                .updatedAt(fragmentDO.getUpdatedAt());
    }

}
