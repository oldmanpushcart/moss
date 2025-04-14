package io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.impl;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.embedding.text.Embedding;
import io.github.oldmanpushcart.dashscope4j.api.embedding.text.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.api.embedding.text.EmbeddingOptions;
import io.github.oldmanpushcart.dashscope4j.api.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.moss.backend.knowledge.KnowledgeConfig;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.dao.KnowledgeDocumentDao;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.dao.KnowledgeSegmentDao;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeDocumentDO;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeSegmentDO;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.DocumentManager;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.QueryDocumentManager;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.impl.splitter.TextSplitByRecursiveCharacter;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.impl.splitter.TextSplitter;
import io.github.oldmanpushcart.moss.util.ExceptionUtils;
import io.github.oldmanpushcart.moss.util.FloatVector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.*;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@Component
public class DocumentManagerImpl implements DocumentManager {

    private final KnowledgeConfig config;
    private final KnowledgeDocumentDao documentDao;
    private final KnowledgeSegmentDao segmentDao;
    private final QueryDocumentManager queryDocumentManager;
    private final Tika tika = new Tika();

    private final DashscopeClient dashscope;
    private final TransactionTemplate transactionTemplate;

    @Override
    public String toString() {
        return "moss://backend/knowledge/document-manager";
    }

    @Override
    public CompletionStage<Long> upsert(URI resource) {
        return CompletableFuture
                .supplyAsync(() -> new UpsertContext(resource))
                .thenCompose(this::processUpsertForPreload)
                .thenCompose(this::processUpsertForEmbeddingChunks)
                .thenCompose(this::processUpsertForPersistence)
                .thenApply(context -> context.getCreateDocumentDO().getDocumentId())
                .exceptionallyCompose(ex -> {
                    final var cause = ExceptionUtils.resolveRootCause(ex);
                    if (cause instanceof UnModifyResourceException unModifyEx) {
                        final var existsDocumentDO = unModifyEx.getExists();
                        return completedStage(existsDocumentDO.getDocumentId());
                    } else {
                        return failedStage(ex);
                    }
                });
    }

    // 加载资源
    private CompletionStage<UpsertContext> processUpsertForPreload(UpsertContext context) {

        // 加载已存在文档
        final var exists = queryDocumentManager.getByResource(context.getResource());
        context.setExistsDocumentDO(exists);

        // 加载资源
        URLConnection connection = null;
        try {

            final var resource = context.getResource();
            connection = resource.toURL().openConnection();
            connection.connect();

            final var mime = connection.getContentType();
            final var size = connection.getContentLengthLong();
            final var lastUpdatedAt = Instant.ofEpochMilli(connection.getLastModified());

            /*
             * 检查当前已存在的文档是否和资源匹配
             * 如果发现资源并没有修改，这里需要及时抛出异常中断整个流程
             * 避免后续重开销的操作
             */
            if (null != exists
                && Objects.equals(exists.getResource(), resource)
                && Objects.equals(exists.getResourceMime(), mime)
                && Objects.equals(exists.getResourceLength(), size)
                && Objects.equals(exists.getResourceLastUpdatedAt(), lastUpdatedAt)) {
                throw new UnModifyResourceException(exists);
            }

            // 解析资源内容
            final var content = tika.parseToString(connection.getInputStream())
                    .replaceAll("(\r\n|\n)", "")
                    .trim();

            // 将加载的信息初始化到上下文
            context.setResourceMime(mime);
            context.setResourceLength(size);
            context.setResourceLastUpdatedAt(lastUpdatedAt);
            context.setResourceContent(content);

            return completedStage(context);
        } catch (Throwable ex) {
            return failedStage(ex);
        } finally {
            if (connection instanceof HttpURLConnection httpURLConnection) {
                httpURLConnection.disconnect();
            }
        }

    }


    // 分割资源为多个资源片段并计算嵌入向量
    private CompletionStage<UpsertContext> processUpsertForEmbeddingChunks(UpsertContext context) {
        final var splitter = new TextSplitByRecursiveCharacter(
                config.getChunk().getSize(),
                config.getChunk().getOverlap()
        );
        final var chunks = splitter.split(context.getResourceContent());
        CompletionStage<UpsertContext> stage = completedFuture(context);
        for (int index = 0; index < chunks.size(); index += 10) {
            final var subChunks = chunks.subList(index, Math.min(index + 10, chunks.size()));
            stage = stage.thenCompose(unused -> processUpsertDocumentForEmbeddingSubChunks(context, subChunks));
        }
        return stage;
    }

    // 计算资源片段的嵌入向量
    private CompletionStage<UpsertContext> processUpsertDocumentForEmbeddingSubChunks(UpsertContext context, List<TextSplitter.Chunk> subChunks) {
        final var content = context.getResourceContent();
        final var subContents = subChunks.stream()
                .map(chunk -> content.substring(chunk.start(), chunk.end()))
                .toList();
        final var request = EmbeddingRequest.newBuilder()
                .option(EmbeddingOptions.TEXT_TYPE, EmbeddingOptions.TextType.DOCUMENT)
                .model(EmbeddingModel.TEXT_EMBEDDING_V3)
                .documents(subContents)
                .build();
        return dashscope.embedding().text().async(request)
                .thenApply(response -> response.output().embeddings())
                .thenApply(embeddings -> {
                    final var subVectors = embeddings.stream()
                            .map(Embedding::vector)
                            .map(FloatVector::new)
                            .toList();
                    for (int index = 0; index < subVectors.size(); index++) {
                        final var chunk = subChunks.get(index);
                        final var vector = subVectors.get(index);
                        context.getChunkMap().put(chunk, vector);
                    }
                    return context;
                });
    }

    // 持久化资源
    private CompletionStage<UpsertContext> processUpsertForPersistence(UpsertContext context) {
        transactionTemplate.executeWithoutResult(status -> {
            try {

                // 如果有已存在的文档，则先进行清理
                if (null != context.getExistsDocumentDO()) {
                    final var exists = context.getExistsDocumentDO();
                    documentDao.deleteById(exists.getDocumentId());
                    segmentDao.deleteByDocumentId(exists.getDocumentId());
                }

                // 创建文档
                final var documentDO = new KnowledgeDocumentDO() {{
                    setResource(context.getResource());
                    setResourceMime(context.getResourceMime());
                    setResourceLength(context.getResourceLength());
                    setResourceLastUpdatedAt(context.getResourceLastUpdatedAt());
                    setResourceContent(context.getResourceContent());
                }};
                documentDao.insert(documentDO);

                // 创建文档片段
                final var segmentDOs = context.getChunkMap().entrySet().stream()
                        .<KnowledgeSegmentDO>map(entry ->
                                new KnowledgeSegmentDO() {{
                                    final var chunk = entry.getKey();
                                    final var vector = entry.getValue();
                                    setDocumentId(documentDO.getDocumentId());
                                    setPosition(chunk.position());
                                    setLength(chunk.length());
                                    setVector(vector);
                                }})
                        .toList();
                segmentDao.inserts(segmentDOs);

                documentDO.setSegmentDOs(segmentDOs);
                context.setCreateDocumentDO(documentDO);

            } catch (Throwable ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });
        return completedStage(context);
    }


    @Override
    public void deleteById(long documentId) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                documentDao.deleteById(documentId);
                segmentDao.deleteByDocumentId(documentId);
            } catch (Throwable ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });
    }

    /**
     * 插入更新上下文
     */
    @Data
    private static class UpsertContext {
        private final URI resource;
        private String resourceMime;
        private Long resourceLength;
        private String resourceContent;
        private Instant resourceLastUpdatedAt;
        private final Map<TextSplitter.Chunk, FloatVector> chunkMap = new HashMap<>();
        private KnowledgeDocumentDO existsDocumentDO;
        private KnowledgeDocumentDO createDocumentDO;
    }

    /**
     * 未修改的资源异常
     * <p>
     * 该异常用来标记资源和文档匹配，无需重建文档
     * </p>
     */
    @EqualsAndHashCode(callSuper = true)
    @Value
    private static class UnModifyResourceException extends RuntimeException {
        KnowledgeDocumentDO exists;
    }

}
