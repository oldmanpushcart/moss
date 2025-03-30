package io.github.oldmanpushcart.moss.infra.memory;

import java.util.List;

/**
 * 记忆体
 */
public interface Memory {

    /**
     * @return 回忆对话消息列表
     */
    List<MemoryFragment> recall();

    List<MemoryFragment> recall(Long maxFragmentId);

    void saveOrUpdate(MemoryFragment fragment);

}
