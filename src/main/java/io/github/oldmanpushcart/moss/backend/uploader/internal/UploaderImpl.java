package io.github.oldmanpushcart.moss.backend.uploader.internal;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.github.oldmanpushcart.moss.backend.uploader.Uploader;
import io.github.oldmanpushcart.moss.backend.uploader.UploaderConfig;
import io.github.oldmanpushcart.moss.backend.uploader.internal.dao.UploadEntryDao;
import io.github.oldmanpushcart.moss.backend.uploader.internal.domain.UploadEntryDO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.moss.backend.uploader.internal.UploadEntryHelper.*;
import static java.util.concurrent.CompletableFuture.completedStage;

/**
 * 上传器默认实现
 */
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class UploaderImpl implements Uploader {

    private final UploaderConfig config;
    private final UploadEntryDao store;
    private final DashscopeClient dashscope;

    @Override
    public String toString() {
        return "moss://infra/uploader";
    }

    @Override
    public CompletionStage<Entry> upload(Model model, URI source) {

        final var filename = source.toASCIIString();
        final var uniqueKey = computeUniqueKey(model, filename);
        final var existDO = store.getByUniqueKey(uniqueKey);
        if (null != existDO) {
            log.debug("{}/upload entry cached, uniqueKey={};model={};source={};",
                    this,
                    uniqueKey,
                    model,
                    source
            );
            return completedStage(toUploadEntry(existDO));
        }

        return _upload(model, source)
                .thenApply(entryDO -> {
                    store.insert(entryDO);
                    return store.getById(entryDO.getEntryId());
                })
                .thenApply(UploadEntryHelper::toUploadEntry)
                .whenComplete((entry,ex)-> {
                    if(null != ex) {
                        log.warn("{}/upload failed, model={};source={};", this, model, source, ex);
                    } else {
                        log.debug("{}/upload success, uniqueKey={};model={};source={};", this, uniqueKey, model, source);
                    }
                });
    }

    private CompletionStage<UploadEntryDO> _upload(Model model, URI source) {
        final var filename = source.toASCIIString();
        if (isQwenLong(model)) {
            return dashscope.base().files()
                    .create(source, source.toASCIIString(), Purpose.FILE_EXTRACT)
                    .thenApply(meta ->
                            new UploadEntryDO()
                                    .setUniqueKey(computeUniqueKey(model, filename))
                                    .setModel(computeModel(model))
                                    .setLength(meta.size())
                                    .setFilename(filename)
                                    .setUploadId(meta.identity())
                                    .setUploaded(meta.toURI())
                                    .setCreatedAt(meta.uploadedAt()));
        } else {
            return dashscope.base().store()
                    .upload(source, model)
                    .thenApply(uploaded -> {

                        URLConnection connection = null;
                        try {

                            connection = source.toURL().openConnection();
                            connection.setConnectTimeout(30 * 1000);
                            connection.setReadTimeout(30 * 1000);
                            connection.connect();

                            return new UploadEntryDO()
                                    .setUniqueKey(computeUniqueKey(model, filename))
                                    .setModel(computeModel(model))
                                    .setLength(connection.getContentLengthLong())
                                    .setFilename(filename)
                                    .setUploaded(uploaded)
                                    .setCreatedAt(Instant.now());

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            if (connection instanceof HttpURLConnection httpURLConnection) {
                                httpURLConnection.disconnect();
                            }
                        }

                    });
        }
    }

    @Override
    public List<Entry> listAll() {
        return store.listAll().stream()
                .map(UploadEntryHelper::toUploadEntry)
                .toList();
    }

    private CompletionStage<?> _delete(long entryId) {
        final var existDO = store.getById(entryId);
        if (null == existDO) {
            return completedStage(null);
        }
        store.deleteById(entryId);
        if (isQwenLong(existDO.getModel())) {
            return dashscope.base().files()
                    .delete(existDO.getUploadId());
        }
        return completedStage(null);
    }

    @Override
    public CompletionStage<?> delete(long entryId) {
        return _delete(entryId)
                .whenComplete((unused, ex)-> {
                    if (null != ex) {
                        log.warn("{}/delete failed, entryId={};", this, entryId, ex);
                    } else {
                        log.debug("{}/delete success, entryId={};", this, entryId);
                    }
                });
    }

    @Override
    public CompletionStage<?> deleteByIds(List<Long> entryIds) {
        CompletionStage<?> stage = completedStage(null);
        for (final var entryId : entryIds) {
            stage = stage.thenCompose(v -> _delete(entryId));
        }
        return stage.whenComplete((unused, ex) -> {
            if (null != ex) {
                log.warn("{}/deleteByIds failed, entryIds={};", this, entryIds, ex);
            } else {
                log.debug("{}/deleteByIds success, entryIds={};", this, entryIds);
            }
        });
    }

    @Override
    public CompletionStage<List<Entry>> flush() {
        return dashscope.base().files().flow()
                .map(UploadEntryHelper::toUploadedEntryDO)
                .collect(Collectors.toList())
                .toCompletionStage()
                .thenAccept(remoteDOs -> {

                    final var remoteDOMap = remoteDOs.stream()
                            .collect(Collectors.toMap(UploadEntryDO::getUniqueKey, remoteDO -> remoteDO));

                    // 删除本地存在但云端不存在的数据
                    store.listAll().stream()
                            .filter(existDO -> isQwenLong(existDO.getModel()))
                            .filter(existDO -> !remoteDOMap.containsKey(existDO.getUniqueKey()))
                            .forEach(existDO -> store.deleteById(existDO.getEntryId()));

                    // 将云端有的数据刷入本地
                    remoteDOs.forEach(remoteDO -> {
                        final var existDO = store.getByUniqueKey(remoteDO.getUniqueKey());
                        if (null != existDO) {
                            store.deleteById(existDO.getEntryId());
                        }
                        store.insert(remoteDO);
                    });

                })
                .thenApply(unused -> listAll())
                .whenComplete((entries, ex) -> {
                    if (null != ex) {
                        log.warn("{}/flush failed;", this, ex);
                    } else {
                        log.debug("{}/flush success, total={};", this, entries.size());
                    }
                });
    }

}
