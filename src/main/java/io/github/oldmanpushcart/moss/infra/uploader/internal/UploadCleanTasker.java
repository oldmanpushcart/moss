package io.github.oldmanpushcart.moss.infra.uploader.internal;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.moss.infra.uploader.UploaderConfig;
import io.github.oldmanpushcart.moss.infra.uploader.internal.dao.UploadEntryDao;
import io.github.oldmanpushcart.moss.infra.uploader.internal.domain.UploadEntryDO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static io.github.oldmanpushcart.moss.infra.uploader.internal.UploadEntryHelper.isQwenLong;

@Slf4j
@Component
public class UploadCleanTasker {

    private static final String NAME = "moss://infra/uploader/clean-tasker";

    private final UploadEntryDao store;
    private final DashscopeClient dashscope;
    private final BlockingQueue<Long> cleanQueue;

    private final Thread tasker = new Thread(this::executeTask, NAME);

    @Autowired
    public UploadCleanTasker(UploaderConfig config, UploadEntryDao store, DashscopeClient dashscope) {
        this.store = store;
        this.dashscope = dashscope;
        this.cleanQueue = new LinkedBlockingQueue<>(config.getCleanQueueCapacity());
    }

    @Override
    public String toString() {
        return NAME;
    }

    public boolean notifyToClean(long entryId) {
        return cleanQueue.offer(entryId);
    }

    private void executeTask() {
        while (!tasker.isInterrupted()) {

            try {

                final var entryId = cleanQueue.take();
                final var entryDO = store.getById(entryId);

                // 如果记录已被删除，则忽略
                if (null == entryDO) {
                    continue;
                }

                /*
                 * QwenLong模型的上传清理需要删除已上传的资源
                 */
                if (isQwenLong(entryDO.getModel())) {
                    cleanQwenLong(entryDO);
                }

                // 最后删除数据即可
                try {
                    store.deleteById(entryId);
                    log.debug("{} clean entry success, entry={};", this, entryId);
                } catch (Throwable ex) {
                    log.error("{} clean entry failed, entry={};", this, entryId, ex);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable ex) {
                log.error("{} clean failed.", this, ex);
            }

        }
    }

    private void cleanQwenLong(UploadEntryDO entryDO) {

        final var entryId = entryDO.getEntryId();

        // 如果没有上传记录，则放弃
        if (null == entryDO.getUpload()) {
            log.debug("{}/qwen-long ignore non-upload, entry={};", this, entryId);
            return;
        }

        final var upload = entryDO.getUpload();
        final var fileId = upload.getHost();
        dashscope.base().files()
                .delete(fileId)
                .handle((ret, ex) -> {
                    log.debug("{}/qwen-long clean completed, entry={};upload={};", this, entryId, upload, ex);
                    return ret;
                })
                .toCompletableFuture()
                .join();

    }

    @PostConstruct
    public void bootstrap() {
        tasker.setDaemon(true);
        tasker.start();
        log.debug("{} bootstrap!", this);
    }

    @PreDestroy
    public void shutdown() {
        tasker.interrupt();
        log.debug("{} shutdown!", this);
    }

}
