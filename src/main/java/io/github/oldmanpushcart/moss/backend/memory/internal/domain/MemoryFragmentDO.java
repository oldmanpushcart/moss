package io.github.oldmanpushcart.moss.backend.memory.internal.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * 记忆片段数据
 */
@Data
@ToString
@EqualsAndHashCode
@Accessors(chain = true)
public class MemoryFragmentDO {

    /**
     * 记忆片段ID
     */
    private Long fragmentId;

    /**
     * 对话消耗的token数
     */
    private Long tokens;

    /**
     * 对话请求消息JSON
     */
    private String requestMessageJson;

    /**
     * 对话应答消息JSON
     */
    private String responseMessageJson;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 修改时间
     */
    private Instant updatedAt;

}
