package io.github.oldmanpushcart.moss.backend.knowledge.internal.dao;

import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeMatchedItemDO;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeSegmentDO;
import io.github.oldmanpushcart.moss.util.FloatVector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeSegmentDao {

    /**
     * 批量插入
     *
     * @param segments 片段集合
     */
    void inserts(
            @Param("segments") List<KnowledgeSegmentDO> segments
    );

    /**
     * 根据文档ID查询片段集合
     *
     * @param documentId 文档ID
     * @return 片段集合
     */
    List<KnowledgeSegmentDO> queryByDocumentId(
            @Param("documentId") long documentId,
            @Param("isIncludeVector") boolean isIncludeVector
    );

    /**
     * 根据文档ID删除片段集合
     *
     * @param documentId 文档ID
     * @return 被删除条数
     */
    int deleteByDocumentId(
            @Param("documentId") long documentId
    );

    /**
     * 向量匹配片段
     *
     * @param query 查询向量
     * @param limit 匹配条数
     * @return 片段集合
     */
    List<KnowledgeMatchedItemDO> matches(
            @Param("query") FloatVector query,
            @Param("limit") int limit
    );

}
