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
     * @return 上传操作结果
     */
    CompletionStage<Entry> upload(Model model, URI source);

    /**
     * 列出所有已上传缓存条目
     *
     * @return 已上传缓存条目集合
     */
    List<Entry> listAll();

    /**
     * 删除缓存条目
     *
     * @param entryId 条目ID
     * @return 删除操作结果
     */
    CompletionStage<?> delete(long entryId);

    CompletionStage<?> deleteByIds(List<Long> entryIds);

    /**
     * 刷新缓存条目
     *
     * @return 刷新操作结果
     */
    CompletionStage<List<Entry>> flush();

    /**
     * 上传条目
     *
     * @param entryId   条目ID
     * @param uniqueKey 上传唯一键
     * @param model     使用模型
     * @param length    文件大小
     * @param filename  文件名称
     * @param uploaded  上传地址
     * @param expiresAt 过期时间
     * @param createdAt 创建时间
     */
    record Entry(
            long entryId,
            String uniqueKey,
            String model,
            long length,
            String filename,
            URI uploaded,
            Instant expiresAt,
            Instant createdAt
    ) {

    }

}
