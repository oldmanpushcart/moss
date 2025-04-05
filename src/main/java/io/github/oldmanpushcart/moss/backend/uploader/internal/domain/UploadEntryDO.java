package io.github.oldmanpushcart.moss.backend.uploader.internal.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * 上传条目数据实体
 */
@Data
@ToString
@EqualsAndHashCode
@Accessors(chain = true)
public class UploadEntryDO {

    /**
     * 条目实体ID
     */
    private Long entryId;

    /**
     * 上传唯一键
     */
    private String uniqueKey;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 资源大小
     */
    private Long length;

    /**
     * 资源地址
     */
    private String filename;

    /**
     * 上传ID
     */
    private String uploadId;

    /**
     * 上传地址
     */
    private URI uploaded;

    /**
     * 过期时间
     */
    private Instant expiresAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 是否过期
     *
     * @return TRUE | FALSE
     */
    public boolean isExpired() {
        return Objects.nonNull(expiresAt)
               && expiresAt.isBefore(Instant.now());
    }

}
