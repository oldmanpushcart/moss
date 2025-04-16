package io.github.oldmanpushcart.moss.backend.memory.internal;

import io.github.oldmanpushcart.moss.backend.memory.Memory;
import io.github.oldmanpushcart.moss.backend.memory.MemoryConfig;
import io.github.oldmanpushcart.moss.backend.memory.internal.manager.MemoryFragmentManager;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.github.oldmanpushcart.moss.backend.memory.internal.MemoryHelper.toMemoryFragment;
import static io.github.oldmanpushcart.moss.backend.memory.internal.MemoryHelper.toMemoryFragmentDO;

/**
 * 记忆体实现
 */
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class MemoryImpl implements Memory {

    private final MemoryConfig config;
    private final MemoryFragmentManager memoryFragmentManager;

    @Override
    public List<Fragment> recall() {
        return recall(Long.MAX_VALUE);
    }

    @Override
    public List<Fragment> recall(long beginFragmentId) {

        final var fragments = new ArrayList<Fragment>();
        long tokens = 0;

        final var fragmentDOIt = memoryFragmentManager.iterator(beginFragmentId);
        while (fragmentDOIt.hasNext()) {

            final var fragmentDO = fragmentDOIt.next();

            if (config.getMaxCount() <= fragments.size()
                || config.getMaxTokens() <= tokens
                || config.getMaxDuration().compareTo(Duration.between(fragmentDO.getCreatedAt(), Instant.now())) <= 0) {
                break;
            }

            tokens += fragmentDO.getTokens();
            fragments.add(toMemoryFragment(fragmentDO));

        }

        Collections.sort(fragments);
        return fragments;
    }

    @Override
    public void persist(Fragment fragment) {

        final var upsertDO = toMemoryFragmentDO(fragment);
        memoryFragmentManager.saveOrUpdate(upsertDO);

        final var fragmentId = upsertDO.getFragmentId();
        final var commitedDO = memoryFragmentManager.getById(fragmentId);
        fragment.self()
                .setFragmentId(commitedDO.getFragmentId())
                .setTokens(commitedDO.getTokens())
                .setCreatedAt(commitedDO.getCreatedAt())
                .setUpdatedAt(commitedDO.getUpdatedAt());

    }


}
