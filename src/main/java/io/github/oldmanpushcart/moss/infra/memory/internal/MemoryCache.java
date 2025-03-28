package io.github.oldmanpushcart.moss.infra.memory.internal;

import io.github.oldmanpushcart.moss.infra.memory.MemoryConfig;
import io.github.oldmanpushcart.moss.infra.memory.MemoryFragment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static io.github.oldmanpushcart.moss.util.CommonUtils.min;
import static io.github.oldmanpushcart.moss.util.CommonUtils.testIfNonNull;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * 记忆缓存
 */
class MemoryCache {

    // 记忆体配置
    private final MemoryConfig config;

    // 存储记忆片段排序队列
    private final TreeSet<MemoryFragment> tree = new TreeSet<>();

    // 缓存中所有记忆片段的总token数
    private volatile long tokens = 0L;

    // 缓存中所有记忆片段的最早创建时间
    private volatile Instant earliest = Instant.now();

    /**
     * 构建记忆体片段缓存
     *
     * @param config 记忆体配置
     */
    public MemoryCache(MemoryConfig config) {
        this.config = config;
    }

    /**
     * 判断是否溢出
     *
     * @return TRUE | FALSE
     */
    public synchronized boolean isOverflow() {
        return isNull(config)
               || testIfNonNull(config.getMaxCount(), v -> tree.size() >= v)
               || testIfNonNull(config.getMaxTokens(), v -> tokens >= v)
               || testIfNonNull(config.getDuration(), v -> earliest.isBefore(Instant.now().minus(v)));
    }

    /*
     * 测试如果添加是否会溢出
     */
    private boolean testOverflow(MemoryFragment fragment) {
        return isNull(config)
               || testIfNonNull(config.getMaxCount(), v -> tree.size() + 1 >= v)
               || testIfNonNull(config.getMaxTokens(), v -> tokens + fragment.tokens() >= v)
               || testIfNonNull(config.getDuration(), v -> fragment.createdAt().isBefore(Instant.now().minus(v)));
    }

    /**
     * 添加记忆片段
     *
     * @param fragment 记忆片段
     */
    public synchronized void append(MemoryFragment fragment) {
        tree.add(fragment);
        tokens += fragment.tokens();
        earliest = min(earliest, fragment.createdAt());
        evict();
    }

    /*
     * 移除最早的记忆片段
     * 并重新计算缓存中记忆片段的总tokens和最早创建时间
     */
    private synchronized void evict() {
        while (!tree.isEmpty() && isOverflow()) {
            final var poll = requireNonNull(tree.pollFirst());
            tokens -= poll.tokens();
            earliest = !tree.isEmpty()
                    ? min(earliest, requireNonNull(tree.first()).createdAt())
                    : Instant.now();
        }
    }

    /**
     * @return 获取所有记忆片段
     */
    public synchronized List<MemoryFragment> elements() {
        return new ArrayList<>(tree);
    }

    /**
     * 加载记忆片段
     *
     * @param loader 加载器
     * @return 本次加载是否已经达到缓存的容量，如果返回FALSE，则需要停止加载
     */
    public boolean load(Loader loader) {
        for (final var fragment : loader.load()) {
            if (testOverflow(fragment)) {
                return false;
            }
            append(fragment);
        }
        return true;
    }

    /**
     * 加载器
     */
    interface Loader {

        /**
         * 加载
         *
         * @return 记忆片段列表
         */
        List<MemoryFragment> load();

    }

}
