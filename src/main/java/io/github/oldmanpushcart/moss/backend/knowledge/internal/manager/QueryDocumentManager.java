package io.github.oldmanpushcart.moss.backend.knowledge.internal.manager;

import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeDocumentDO;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeMatchedItemDO;
import lombok.Data;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 查询知识库文档
 */
public interface QueryDocumentManager {

    /**
     * 根据ID查询知识库文档
     *
     * @param documentId 文档ID
     * @param option     查询选项
     * @return 文档
     */
    KnowledgeDocumentDO getById(long documentId, QueryOption option);

    /**
     * 根据资源查询知识库文档
     *
     * @param documentId 文档ID
     * @return 文档
     */
    default KnowledgeDocumentDO getById(long documentId) {
        return getById(documentId, new QueryOption());
    }

    /**
     * 根据资源查询知识库文档
     *
     * @param resource 资源
     * @param option   查询选项
     * @return 文档
     */
    KnowledgeDocumentDO getByResource(URI resource, QueryOption option);

    /**
     * 根据资源查询知识库文档
     *
     * @param resource 资源
     * @return 文档
     */
    default KnowledgeDocumentDO getByResource(URI resource) {
        return getByResource(resource, new QueryOption());
    }

    /**
     * 分页查询知识库文档
     *
     * @param pageIndex 页码（从1开始）
     * @param pageSize  页大小
     * @param option    查询选项
     * @return 文档列表
     */
    List<KnowledgeDocumentDO> queryByPage(int pageIndex, int pageSize, QueryOption option);

    /**
     * 分页查询知识库文档
     *
     * @param pageIndex 页码（从1开始）
     * @param pageSize  页大小
     * @return 文档列表
     */
    default List<KnowledgeDocumentDO> queryByPage(int pageIndex, int pageSize) {
        return queryByPage(pageIndex, pageSize, new QueryOption());
    }

    /**
     * 匹配
     *
     * @param query    查询
     * @param limit    限制数量
     * @param distance 距离
     * @return 匹配结果
     */
    CompletionStage<List<KnowledgeMatchedItemDO>> matches(String query, int limit, float distance);

    /**
     * 查询选项
     */
    @Data
    class QueryOption {

        /**
         * 是否包含文档内容
         */
        private boolean includeDocumentContent = false;

        /**
         * 是否包含知识库片段
         */
        private boolean includeSegmentDOs = false;

        /**
         * 是否包含知识库片段向量
         */
        private boolean includeSegmentVector = false;

    }

}
