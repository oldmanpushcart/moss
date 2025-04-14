package io.github.oldmanpushcart.moss.backend.knowledge.internal.domain;

import io.github.oldmanpushcart.moss.util.FloatVector;
import lombok.Data;

import java.time.Instant;

/**
 * 知识库文档片段
 */
@Data
public class KnowledgeSegmentDO {

    /**
     * 文档片段ID
     */
    private Long segmentId;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 内容起始位置
     */
    private Integer position;

    /**
     * 内容长度
     */
    private Integer length;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 内容向量
     */
    private FloatVector vector;

}
