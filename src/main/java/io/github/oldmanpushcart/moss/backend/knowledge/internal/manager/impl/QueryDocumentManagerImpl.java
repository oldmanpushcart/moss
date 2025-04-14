package io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.impl;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.embedding.text.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.api.embedding.text.EmbeddingOptions;
import io.github.oldmanpushcart.dashscope4j.api.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.moss.backend.knowledge.KnowledgeConfig;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.dao.KnowledgeDocumentDao;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.dao.KnowledgeSegmentDao;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeDocumentDO;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeMatchedItemDO;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.QueryDocumentManager;
import io.github.oldmanpushcart.moss.util.FloatVector;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class QueryDocumentManagerImpl implements QueryDocumentManager {

    private final KnowledgeConfig config;
    private final KnowledgeDocumentDao documentDao;
    private final KnowledgeSegmentDao segmentDao;
    private final DashscopeClient dashscope;

    @Override
    public KnowledgeDocumentDO getById(long documentId, QueryOption option) {
        final var documentDO = documentDao.getById(documentId, option.isIncludeDocumentContent());
        if (null == documentDO) {
            return null;
        }
        fillSegmentDOsIfNeeded(documentDO, option);
        return documentDO;
    }

    @Override
    public KnowledgeDocumentDO getByResource(URI resource, QueryOption option) {
        final var documentDO = documentDao.getByResource(resource, option.isIncludeDocumentContent());
        if (null == documentDO) {
            return null;
        }
        fillSegmentDOsIfNeeded(documentDO, option);
        return documentDO;
    }

    @Override
    public List<KnowledgeDocumentDO> queryByPage(int pageIndex, int pageSize, QueryOption option) {
        final var offset = (pageIndex - 1) * pageSize;
        final var documentDOs = documentDao.queryByPage(offset, pageSize, option.isIncludeDocumentContent());
        documentDOs.forEach(documentDO -> fillSegmentDOsIfNeeded(documentDO, option));
        return documentDOs;
    }

    private void fillSegmentDOsIfNeeded(KnowledgeDocumentDO documentDO, QueryOption option) {
        if (!option.isIncludeSegmentDOs()) {
            return;
        }
        final var segmentDOs = segmentDao.queryByDocumentId(documentDO.getDocumentId(), option.isIncludeSegmentVector());
        documentDO.setSegmentDOs(segmentDOs);
    }

    @Override
    public CompletionStage<List<KnowledgeMatchedItemDO>> matches(String query, int limit, float distance) {
        return computeQueryVector(query)
                .thenApply(v -> processMatchesForQueryVector(v, limit))
                .thenApply(v -> processMatchesForRelevantContent(v, distance));
    }

    // 计算查询向量
    private CompletionStage<FloatVector> computeQueryVector(String query) {
        final var request = EmbeddingRequest.newBuilder()
                .option(EmbeddingOptions.TEXT_TYPE, EmbeddingOptions.TextType.QUERY)
                .model(EmbeddingModel.TEXT_EMBEDDING_V3)
                .addDocument(query)
                .build();
        return dashscope.embedding().text().async(request)
                .thenApply(response -> response.output().embeddings())
                .thenApply(embeddings -> embeddings.get(0))
                .thenApply(embedding -> new FloatVector(embedding.vector()));
    }

    private List<KnowledgeMatchedItemDO> processMatchesForQueryVector(FloatVector queryVector, int limit) {
        return segmentDao.matches(queryVector, limit);
    }

    // 获取片段内容
    private List<KnowledgeMatchedItemDO> processMatchesForRelevantContent(List<KnowledgeMatchedItemDO> matchedItemDOs, float distance) {
        return matchedItemDOs.stream()
                .filter(matchedItemDO -> matchedItemDO.getDistance() <= distance)
                .peek(matchedItemDO -> {
                    final var documentId = matchedItemDO.getDocumentId();
                    final var position = matchedItemDO.getPosition();
                    final var length = matchedItemDO.getLength();
                    final var content = documentDao.relevantContent(documentId, position, length);
                    matchedItemDO.setContent(content);
                })
                .toList();
    }

}
