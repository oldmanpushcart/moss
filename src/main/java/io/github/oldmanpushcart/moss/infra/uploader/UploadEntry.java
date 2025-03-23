package io.github.oldmanpushcart.moss.infra.uploader;

import java.net.URI;

/**
 * 上传条目
 *
 * @param entryId 条目ID
 * @param upload  上传地址
 * @param mime    资源类型
 * @param length  资源大小
 */
public record UploadEntry(
        long entryId,
        URI upload,
        String mime,
        long length
) {

}
