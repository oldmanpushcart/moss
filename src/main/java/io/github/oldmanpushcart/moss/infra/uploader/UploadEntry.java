package io.github.oldmanpushcart.moss.infra.uploader;

import java.net.URI;
import java.time.Instant;

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
public record UploadEntry(
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
