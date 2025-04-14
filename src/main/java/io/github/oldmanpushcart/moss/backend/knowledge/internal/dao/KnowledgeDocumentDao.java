package io.github.oldmanpushcart.moss.backend.knowledge.internal.dao;

import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeDocumentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.net.URI;
import java.util.List;

@Mapper
public interface KnowledgeDocumentDao {

    /**
     * 插入知识库文档
     *
     * @param document 文档
     */
    void insert(KnowledgeDocumentDO document);

    /**
     * 根据ID获取文档
     *
     * @param documentId       文档ID
     * @param isIncludeContent 是否查询文档内容
     * @return 文档
     */
    KnowledgeDocumentDO getById(
            @Param("documentId") long documentId,
            @Param("isIncludeContent") boolean isIncludeContent
    );

    /**
     * 根据资源获取文档
     *
     * @param resource         文档资源
     * @param isIncludeContent 是否查询文档内容
     * @return 文档
     */
    KnowledgeDocumentDO getByResource(
            @Param("resource") URI resource,
            @Param("isIncludeContent") boolean isIncludeContent
    );

    List<KnowledgeDocumentDO> queryByPage(
            @Param("offset") int pageIndex,
            @Param("limit") int pageSize,
            @Param("isIncludeContent") boolean isIncludeContent
    );


    /**
     * 根据ID删除文档
     *
     * @param documentId 文档ID
     * @return 删除条数
     */
    int deleteById(
            @Param("documentId") long documentId
    );

    /**
     * 根据ID获取文档内容片段
     *
     * @param documentId 文档ID
     * @param position   起始位置
     * @param length     长度
     * @return 文档内容片段文本
     */
    String relevantContent(
            @Param("documentId") long documentId,
            @Param("position") int position,
            @Param("length") int length
    );

}
