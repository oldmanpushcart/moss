package io.github.oldmanpushcart.moss.backend.knowledge.internal.domain;

import lombok.Data;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * 知识库文档
 */
@Data
public class KnowledgeDocumentDO {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 文档资源
     */
    private URI resource;

    /**
     * 资源类型
     */
    private String resourceMime;

    /**
     * 资源长度
     */
    private Long resourceLength;

    /**
     * 资源最后更新时间
     */
    private Instant resourceLastUpdatedAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 资源内容
     */
    private String resourceContent;

    /**
     * 关联的分片
     */
    private List<KnowledgeSegmentDO> segmentDOs;

}
