package io.github.oldmanpushcart.moss.backend.knowledge.internal.manager;

import java.net.URI;
import java.util.concurrent.CompletionStage;

/**
 * 文档管理器
 */
public interface DocumentManager {

    /**
     * 创建或更新(重建)文档
     *
     * @param resource 资源
     * @return 文档ID
     */
    CompletionStage<Long> upsert(URI resource);

    /**
     * 删除文档
     *
     * @param documentId 文档ID
     */
    void deleteById(long documentId);

}
