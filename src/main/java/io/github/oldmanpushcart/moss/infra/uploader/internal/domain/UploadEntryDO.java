package io.github.oldmanpushcart.moss.infra.uploader.internal.domain;

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
     * 状态：已创建
     */
    public static final int STATUS_CREATED = 1;

    /**
     * 状态：已上传
     */
    public static final int STATUS_UPLOADED = 2;

    /**
     * 状态：已删除
     */
    public static final int STATUS_DELETED = -1;

    /**
     * 条目实体ID
     */
    private Long entryId;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 资源类型
     */
    private String mime;

    /**
     * 资源大小
     */
    private Long length;

    /**
     * 资源地址
     */
    private URI source;

    /**
     * 上传地址
     */
    private URI upload;

    /**
     * 上传KEY
     */
    private String uploadKey;

    /**
     * 条目状态
     */
    private Integer status;

    /**
     * 过期时间
     */
    private Instant expiresAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 版本号
     */
    private Integer version;

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
