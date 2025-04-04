package io.github.oldmanpushcart.moss.backend.uploader;

import io.github.oldmanpushcart.dashscope4j.Model;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 上传器
 */
public interface Uploader {

    /**
     * 获取被缓存的条目，否则创建并返回
     *
     * @param model  模型
     * @param source 资源URI
     * @return 缓存条目
     */
    CompletionStage<Entry> upload(Model model, URI source);

    /**
     * 列出所有已上传缓存条目
     *
     * @return 已上传缓存条目集合
     */
    List<Entry> listUploaded();

    /**
     * 删除缓存条目
     *
     * @param entryId 条目ID
     * @return 删除是否成功
     */
    boolean delete(long entryId);

    /**
     * 删除缓存条目
     *
     * @param model  模型
     * @param source 资源URI
     * @return 删除是否成功
     */
    boolean delete(Model model, URI source);

    /**
     * 上传条目
     *
     * @param entryId 条目ID
     * @param mime    资源类型
     * @param length  资源大小
     * @param model   模型名称
     * @param source  资源地址
     * @param upload  上传地址
     */
    record Entry(
            long entryId,
            String mime,
            long length,
            String model,
            URI source,
            URI upload,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {

    }
}
