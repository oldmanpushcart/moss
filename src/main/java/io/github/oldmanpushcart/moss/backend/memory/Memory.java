package io.github.oldmanpushcart.moss.backend.memory;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

/**
 * 记忆体
 */
public interface Memory {

    /**
     * 获取最大片段ID
     *
     * @return 最大片段ID
     */
    Long getMaxFragmentId();

    /**
     * 回忆
     *
     * @return 回忆起来的记忆片段集合
     */
    List<Fragment> recall(long endFragmentId);

    /**
     * 回忆
     *
     * @param beginFragmentId 开始回忆片段ID
     * @return 回忆起来的记忆片段集合
     */
    List<Fragment> recall(long beginFragmentId, long endFragmentId);

    /**
     * 持久化
     * <p>
     * 将记忆片段持久化，
     * <li>如果是新的记忆，则追加</li>
     * <li>如果是旧的记忆，则更新</li>
     * </p>
     *
     * @param fragment 记忆片段
     */
    void persist(Fragment fragment);

    /**
     * 记忆片段
     */
    @Data
    @Accessors(chain = true)
    class Fragment implements Comparable<Fragment> {

        private Long fragmentId;
        private Long tokens;
        private Message requestMessage;
        private Message responseMessage;
        private Instant createdAt;
        private Instant updatedAt;

        public Fragment self() {
            return this;
        }

        @Override
        public int compareTo(Memory.Fragment o) {
            return Long.compare(this.fragmentId, o.fragmentId);
        }

    }

}
