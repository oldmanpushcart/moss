package io.github.oldmanpushcart.moss.backend.uploader.internal;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.github.oldmanpushcart.moss.backend.uploader.Uploader;
import io.github.oldmanpushcart.moss.backend.uploader.UploaderConfig;
import io.github.oldmanpushcart.moss.backend.uploader.internal.dao.UploadEntryDao;
import io.github.oldmanpushcart.moss.backend.uploader.internal.domain.UploadEntryDO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.moss.backend.uploader.internal.UploadEntryHelper.isQwenLong;
import static io.github.oldmanpushcart.moss.backend.uploader.internal.UploadEntryHelper.toUploadEntry;
import static io.github.oldmanpushcart.moss.backend.uploader.internal.domain.UploadEntryDO.*;
import static java.util.concurrent.CompletableFuture.completedStage;
import static java.util.concurrent.CompletableFuture.failedStage;

/**
 * 上传器默认实现
 */
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class UploaderImpl implements Uploader {

    private final UploaderConfig config;
    private final UploadEntryDao store;
    private final UploadCleanTasker cleaner;
    private final DashscopeClient dashscope;

    @Override
    public String toString() {
        return "moss://infra/uploader";
    }

    @Override
    public CompletionStage<Entry> upload(Model model, URI resource) {

        final var uploadKey = computeUploadKey(model, resource);
        final var existDO = store.getByUploadKey(uploadKey);

        /*
         * 判断是否存在上传条目
         * 1. 没有过期
         * 2. 已上传
         */
        if (null != existDO
            && !existDO.isExpired()
            && existDO.getStatus() == STATUS_UPLOADED) {
            log.debug("{}/upload entry cached, entry={};model={};source={};upload={};",
                    this,
                    existDO.getEntryId(),
                    existDO.getModel(),
                    existDO.getSource(),
                    existDO.getUpload()
            );
            return completedStage(toUploadEntry(existDO));
        }

        // 如果条目已过期，这里进行主动更新
        if (null != existDO && existDO.isExpired()) {
            final var ret = delete(existDO.getEntryId());
            log.debug("{}/upload entry expired, entry={};ret={};", this, existDO.getEntryId(), ret);
        }

        /*
         * 完成上传操作
         * 1. 创建条目：已创建
         * 2. 执行上传
         * 3. 更新条目：已上传
         */
        return completedStage(null)
                .thenCompose(unused -> insert(model, resource))
                .thenCompose(entryDO -> upload(entryDO, model, resource))
                .thenApply(UploadEntryHelper::toUploadEntry)
                .whenComplete((entry, ex) -> {
                    if (null == ex) {
                        log.debug("{}/upload success, model={};source={};upload={};",
                                this,
                                model.name(),
                                resource,
                                entry.upload()
                        );
                    } else {
                        log.debug("{}/upload failed, model={};source={};",
                                this,
                                model.name(),
                                resource,
                                ex
                        );
                    }
                });
    }

    @Override
    public List<Entry> listUploaded() {
        return store.queryByStatus(STATUS_UPLOADED)
                .stream()
                .map(UploadEntryHelper::toUploadEntry)
                .toList();
    }

    /*
     * 插入新条目
     */
    private CompletionStage<UploadEntryDO> insert(Model model, URI resource) {

        /*
         * 这里考虑兼容File、Http等情况，采用了转为URL并获取对应类型、长度的方案
         * 如果是HttpURL最后需要主动断开连接
         */
        URLConnection connection = null;
        try {

            /*
             * 打开URL资源连接
             */
            connection = resource.toURL().openConnection();
            connection.connect();

            /*
             * 根据资源信息构造条目信息
             * 刚创建的条目状态是：已创建，此时是没有上传的
             */
            final var entryDO = new UploadEntryDO()
                    .setModel(model.name())
                    .setMime(connection.getContentType())
                    .setLength(connection.getContentLengthLong())
                    .setSource(resource)
                    .setStatus(STATUS_CREATED)
                    .setVersion(0);
            store.insert(entryDO);

            return completedStage(entryDO);

        } catch (Throwable ex) {
            return failedStage(ex);
        } finally {
            if (connection instanceof HttpURLConnection httpURLConnection) {
                httpURLConnection.disconnect();
            }
        }

    }

    /*
     * 上传资源
     */
    private CompletionStage<UploadEntryDO> upload(UploadEntryDO entryDO, Model model, URI resource) {

        /*
         * 上传根据模型的不同而分两种方案
         * 1. QwenLong：通过dashscope/base/files完成上传，没有过期时间
         * 2. 其他模型：通过dashscope/base/store完成上传，过期时间为48小时
         */
        final CompletionStage<URI> completed;
        if (isQwenLong(model)) {
            completed = dashscope.base()
                    .files()
                    .create(resource, resource.getPath(), Purpose.FILE_EXTRACT)
                    .thenApply(FileMeta::toURI);
        } else {
            entryDO.setExpiresAt(Instant.now().plus(config.getOssExpiresDuration()));
            completed = dashscope.base()
                    .store()
                    .upload(resource, model);
        }

        /*
         * 更新条目数据状态为：已上传
         * 如果上传成功但更新数据库失败，则需要通过后台对账找出该条目，并删除
         */
        return completed.thenApply(upload -> {
            entryDO.setUpload(upload);
            entryDO.setUploadKey(computeUploadKey(model, resource));
            entryDO.setStatus(STATUS_UPLOADED);
            entryDO.setVersion(entryDO.getVersion());
            if (1 != store.update(entryDO)) {
                throw new RuntimeException(
                        "Update Entry: %s to uploaded failed! effect row != 1".formatted(
                                entryDO.getEntryId()
                        ));
            }
            return entryDO;
        });
    }

    // 计算上传KEY
    private static String computeUploadKey(Model model, URI resource) {
        return String.format("%s-%s", model.name(), resource);
    }

    @Override
    public boolean delete(long entryId) {
        final var entryDO = store.getById(entryId);
        return _delete(entryDO);
    }

    @Override
    public boolean delete(Model model, URI source) {
        final var entryKey = computeUploadKey(model, source);
        final var entryDO = store.getByUploadKey(entryKey);
        return _delete(entryDO);
    }

    /*
     * 删除条目数据
     */
    private boolean _delete(UploadEntryDO entryDO) {

        // 如果不存在或者状态为已删除状态，则直接干了
        if (null == entryDO || entryDO.getStatus() == STATUS_DELETED) {
            return true;
        }

        // 构造更新数据
        final var updateDO = new UploadEntryDO()
                .setEntryId(entryDO.getEntryId())
                .setStatus(STATUS_DELETED)
                .setUploadKey("DELETED-%s-%s".formatted(
                        entryDO.getEntryId(),
                        entryDO.getUploadKey()
                ))
                .setVersion(entryDO.getVersion());

        // 更新数据
        final var ret = 1 == store.update(updateDO);
        log.debug("{}/delete completed. entry={};ret={};", this, entryDO.getEntryId(), ret);

        return ret;
    }

    @Scheduled(cron = "#{@uploaderConfig.getCleanCronExpress()}")
    public void scheduleClean() {

        final var beginMs = System.currentTimeMillis();
        final var cleanEntryIds = store.queryForClean(config.getCleanBatchSize())
                .stream()
                .map(UploadEntryDO::getEntryId)
                .toList();
        final var finishMs = System.currentTimeMillis();

        log.debug("{}/schedule-clean scan {} entry waiting for clean cost {}ms.",
                this,
                cleanEntryIds.size(),
                finishMs - beginMs
        );

        for (final var entryId : cleanEntryIds) {
            if (!cleaner.notifyToClean(entryId)) {
                log.debug("{}/schedule-clean notify to clean failed, entry={};", this, entryId);
                break;
            }
        }

    }

}
