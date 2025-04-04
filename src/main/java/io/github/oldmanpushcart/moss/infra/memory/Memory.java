package io.github.oldmanpushcart.moss.infra.memory;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

/**
 * 记忆体
 */
public interface Memory {

    /**
     * @return 回忆对话消息列表
     */
    List<Fragment> recall();

    List<Fragment> recall(Long maxFragmentId);

    void saveOrUpdate(Fragment fragment);

    @Data
    @Accessors(chain = true, fluent = true)
    class Fragment implements Comparable<Fragment> {

        private Long fragmentId;
        private Long tokens;
        private Message requestMessage;
        private Message responseMessage;
        private Instant createdAt;
        private Instant updatedAt;

        @Override
        public int compareTo(@NotNull Memory.Fragment o) {
            return Long.compare(this.fragmentId, o.fragmentId);
        }

    }
}
