package io.github.oldmanpushcart.moss.backend.knowledge.internal;

import io.github.oldmanpushcart.moss.backend.knowledge.KnowledgeConfig;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeDocumentDO;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.DocumentManager;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.QueryDocumentManager;
import io.github.oldmanpushcart.moss.util.PathWatcher;
import io.github.oldmanpushcart.moss.util.WildcardMatcher;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toUnmodifiableSet;

/**
 * 知识库目录监听器
 */
@Slf4j
@Component
public class KnowledgeDirectoryMonitor implements PathWatcher.Listener, InitializingBean, DisposableBean {

    private final KnowledgeConfig config;
    private final DocumentManager documentManager;
    private final QueryDocumentManager queryDocumentManager;

    private final PathWatcher pathWatcher;
    private final Thread monitor = new Thread(this::monitoring);
    private final BlockingQueue<PathDelay> waitingForProcessPathQueue = new DelayQueue<>();

    @Autowired
    public KnowledgeDirectoryMonitor(
            KnowledgeConfig config,
            DocumentManager documentManager,
            QueryDocumentManager queryDocumentManager
    ) {
        this.config = config;
        this.documentManager = documentManager;
        this.queryDocumentManager = queryDocumentManager;
        this.pathWatcher = PathWatcher.newBuilder()
                .root(config.getLocation())
                .listener(this)
                .build();
    }

    @Override
    public String toString() {
        return "moss://backend/knowledge/directory-monitor";
    }

    @Override
    public void onEvent(PathWatcher.Event event) throws IOException {

        log.debug("{} received: event={};", this, event);

        final var path = event.source();
        final var mime = Files.probeContentType(path);
        final var isAllow = config.getAllowMimeWildcards()
                .stream()
                .anyMatch(it -> WildcardMatcher.match(mime, it));

        // 检查MIME是否在允许范围，如果不在则放弃处理
        if (!isAllow) {
            log.debug("{} ignored: mime not allow. mime={};path={};", this, mime, path);
            return;
        }

        /*
         * 添加到待处理队列中，慢慢处理
         * 这里对每一个事件都做一个500ms的延迟处理，主要是屏蔽操作系统、文件系统的差异
         *
         * 在不同的操作系统下对文件变更事件和实际刷盘的时机是不同的，
         * 很有可能在获取到创建事件的时候，实际上文件并没有落盘（或者META区没更新），会导致Files.exists()判断时为false,
         * 为了解决这个问题，所以每个事件延迟一定的时间，等待操作系统完成刷盘后再去核对。
         *
         * 我承认这样做有点取巧，但我也没有找到更好的办法，如果你知道更好的方法，请告知我，谢谢！
         */
        final var expiresAt = Instant.now().plusMillis(500);
        if (!waitingForProcessPathQueue.offer(new PathDelay(path, expiresAt))) {
            log.warn("{} ignored: queue is overflow! path={};", this, path);
        }

    }

    @Override
    public void onError(Path path, Throwable cause) {
        log.warn("{} occur error! path={}", this, path);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        // 删除资源已经不存在的文档
        deleteDocumentIfResourceNotExists();

        // 启动路径监听器
        pathWatcher.start();

        // 启动监听器
        monitor.setName(this.toString());
        monitor.setDaemon(true);
        monitor.start();

        log.debug("{} started!", this);
    }

    private void deleteDocumentIfResourceNotExists() {

        // 等待删除文档集合
        final var waitingDeleteDocumentDOs = new HashSet<KnowledgeDocumentDO>();

        int pageIndex = 1;
        while (true) {

            // 分页查询知识库文档
            final var documentDOs = queryDocumentManager.queryByPage(pageIndex++, 100);
            if (documentDOs.isEmpty()) {
                break;
            }

            /*
             * 遍历本次分页查询出来的文档，检查文档的资源是否还存在
             * 如果不存在则删除对应的文档
             */
            documentDOs.forEach(documentDO -> {

                // 只处理file
                if (!"file".equalsIgnoreCase(documentDO.getResource().getScheme())) {
                    return;
                }

                // 将文件不存在的文档加入删除集合
                if (!Files.exists(Path.of(documentDO.getResource()))) {
                    waitingDeleteDocumentDOs.add(documentDO);
                }

            });

        }

        // 批量删除文档
        for (final var waitingDeleteDocumentDO : waitingDeleteDocumentDOs) {
            final var documentId = waitingDeleteDocumentDO.getDocumentId();
            final var resource = waitingDeleteDocumentDO.getResource();
            documentManager.deleteById(documentId);
            log.debug("{} delete document: not exists resource. document={};resource={};", this, documentId, resource);
        }

    }

    private Set<Path> batchTake() throws InterruptedException {
        final var pathDelaySet = new HashSet<PathDelay>();
        final var pathDelay = waitingForProcessPathQueue.take();
        pathDelaySet.add(pathDelay);
        waitingForProcessPathQueue.drainTo(pathDelaySet);
        return pathDelaySet.stream()
                .map(PathDelay::path)
                .collect(toUnmodifiableSet());
    }

    private void monitoring() {
        while (!Thread.currentThread().isInterrupted()) {
            try {

                // 批量获取等待处理的文件进行处理
                batchTake().forEach(path -> {

                    // 转换为URI资源
                    final var resource = path.toUri();

                    // 不处理文件夹
                    if (Files.isDirectory(path)) {
                        log.debug("{} ignored: is directory. path={};", this, path);
                        return;
                    }

                    // 如果文件已经不存在，则说明是被删除了
                    if (!Files.exists(path)) {

                        /*
                         * 查询知识库中是否有对应文档
                         * 如果没有文档将放弃处理
                         */
                        final var documentDO = queryDocumentManager.getByResource(resource);
                        if (null == documentDO) {
                            return;
                        }

                        // 删除对应文档
                        documentManager.deleteById(documentDO.getDocumentId());
                        log.debug("{} delete document: resource was deleted. document={};resource={};", this, documentDO.getDocumentId(), resource);

                    }

                    // 文件存在，则说明有变更，可能是创建也可能是修改
                    else {
                        documentManager.upsert(resource)
                                .whenComplete((documentId, ex) -> {
                                    if (null == ex) {
                                        log.debug("{} upsert document: success. document={};resource={};", this, documentId, resource);
                                    } else {
                                        log.warn("{} upsert document: failure. document={};resource={};", this, documentId, resource, ex);
                                    }
                                })
                                .toCompletableFuture()
                                .join();
                    }

                });

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable ex) {
                log.warn("{} occur error!", this, ex);
            }
        }
    }

    @Override
    public void destroy() {

        // 停止路径监听器
        pathWatcher.stop();

        // 停止监听器
        monitor.interrupt();

        log.debug("{} destroyed!", this);
    }

    /**
     * 路径延迟
     *
     * @param path      路径
     * @param expiresAt 执行时间
     */
    record PathDelay(Path path, Instant expiresAt) implements Delayed {

        @Override
        public long getDelay(@Nonnull TimeUnit unit) {
            final var diff = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@Nonnull Delayed delayed) {
            if (delayed instanceof PathDelay pathDelay) {
                return this.expiresAt.compareTo(pathDelay.expiresAt);
            } else {
                throw new IllegalArgumentException("not PathDelay");
            }
        }

    }

}
