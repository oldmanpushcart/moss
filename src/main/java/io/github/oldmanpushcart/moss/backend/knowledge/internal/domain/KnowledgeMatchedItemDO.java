package io.github.oldmanpushcart.moss.backend.knowledge.internal.domain;

import lombok.Data;

/**
 * 知识库匹配结果项
 */
@Data
public class KnowledgeMatchedItemDO {

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
     * 匹配距离
     */
    private Float distance;

    /**
     * 片段内容
     */
    private String content;

}
