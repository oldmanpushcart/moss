package io.github.oldmanpushcart.moss.backend.memory.internal;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.LocalTokenizerUtils;
import io.github.oldmanpushcart.dashscope4j.util.MessageCodec;
import io.github.oldmanpushcart.moss.backend.memory.Memory;
import io.github.oldmanpushcart.moss.backend.memory.MemoryConfig;
import io.github.oldmanpushcart.moss.backend.memory.internal.dao.MemoryFragmentDao;
import io.github.oldmanpushcart.moss.backend.memory.internal.domain.MemoryFragmentDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel.Mode.MULTIMODAL;
import static java.util.Collections.unmodifiableList;

/**
 * 记忆体实现
 */
@Component
public class MemoryImpl implements Memory {

    private final MemoryConfig config;
    private final MemoryFragmentDao store;
    private final MemoryCache primaryCache;

    @Autowired
    public MemoryImpl(MemoryConfig config, MemoryFragmentDao store) {
        this.config = config;
        this.store = store;
        this.primaryCache = new MemoryCache(config);
        loadingCache(primaryCache, Long.MAX_VALUE);
    }

    /*
     * 从持久化存储中加载记忆片段
     */
    private void loadingCache(MemoryCache cache, long maxFragmentId) {
        pagingForLoadingCache(cache, maxFragmentId, 100);
    }

    /*
     * 从持久化存储中翻页加载记忆片段
     */
    private void pagingForLoadingCache(MemoryCache cache, long maxFragmentId, int limit) {

        // 如果缓存已满，则不再加载
        if (cache.isOverflow()) {
            return;
        }

        // 从持久化存储中查询一页数据
        final var fragmentDOs = store.pagingForIterator(maxFragmentId, limit);

        // 将一页记忆片段数据加载到缓存中
        if (!cache.load(() -> fragmentDOs.stream().map(MemoryFragmentHelper::fromMemoryFragmentDO).toList())) {
            return;
        }

        // 查询的数据不满足一页，说明没有下一页
        if (fragmentDOs.size() < limit) {
            return;
        }

        // 找到当前页最小的游标，继续翻页查询
        final var minFragmentId = fragmentDOs.get(fragmentDOs.size() - 1).getFragmentId();
        pagingForLoadingCache(cache, minFragmentId, limit);

    }

    @Override
    public List<Fragment> recall() {
        return unmodifiableList(primaryCache.elements());
    }

    @Override
    public List<Fragment> recall(Long maxFragmentId) {
        if (Objects.isNull(maxFragmentId)) {
            return recall();
        }

        final var queryCache = new MemoryCache(config);

        /*
         * 从主缓存中加载记忆片段到查询缓存
         */
        long minFragmentId = maxFragmentId;
        final var fragmentsInCache = primaryCache.elements();
        for (final var fragment : fragmentsInCache) {
            if (fragment.fragmentId() < maxFragmentId
                && queryCache.load(() -> List.of(fragment))) {
                minFragmentId = fragment.fragmentId();
            }
        }

        // 从存储中加载记忆片段到查询缓存
        loadingCache(queryCache, minFragmentId);

        return unmodifiableList(queryCache.elements());
    }

    @Override
    public void saveOrUpdate(Fragment fragment) {

        final var mergedDO = new MemoryFragmentDO()
                .setFragmentId(fragment.fragmentId())
                .setTokens(computeMessageTokens(fragment.requestMessage()) + computeMessageTokens(fragment.responseMessage()))
                .setRequestMessageJson(MessageCodec.encodeToJson(MULTIMODAL, fragment.requestMessage()))
                .setResponseMessageJson(MessageCodec.encodeToJson(MULTIMODAL, fragment.responseMessage()));

        if (Objects.isNull(mergedDO.getFragmentId())) {
            store.insert(mergedDO);
        } else {
            if (store.update(mergedDO) != 1) {
                throw new IllegalStateException("Update memory fragment failed!");
            }
        }

        final var existedDO = store.getById(mergedDO.getFragmentId());
        final var existed = MemoryFragmentHelper.fromMemoryFragmentDO(existedDO);
        primaryCache.append(existed);

        fragment.fragmentId(existedDO.getFragmentId());
        fragment.tokens(existedDO.getTokens());
        fragment.createdAt(existedDO.getCreatedAt());
        fragment.updatedAt(existedDO.getUpdatedAt());

    }

    // 计算消息所消耗的TOKENS
    private static long computeMessageTokens(Message message) {
        return LocalTokenizerUtils.encode(message.text()).size();
    }


}
