package io.github.oldmanpushcart.moss.backend.memory.internal.manager;

import io.github.oldmanpushcart.moss.backend.memory.internal.domain.MemoryFragmentDO;

import java.util.Iterator;

public interface MemoryFragmentManager {

    Long getMaxFragmentId();

    void saveOrUpdate(MemoryFragmentDO fragmentDO);

    MemoryFragmentDO getById(long fragmentId);

    Iterator<MemoryFragmentDO> iterator(long beginFragmentId, long endFragmentId);

}
