package io.github.oldmanpushcart.moss.backend.knowledge.internal;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.backend.knowledge.Knowledge;
import io.github.oldmanpushcart.moss.backend.knowledge.KnowledgeConfig;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.domain.KnowledgeMatchedItemDO;
import io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.QueryDocumentManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.api.chat.message.Message.Role.AI;
import static io.github.oldmanpushcart.dashscope4j.api.chat.message.Message.Role.USER;
import static io.github.oldmanpushcart.moss.util.CommonUtils.isIn;
import static java.util.concurrent.CompletableFuture.completedStage;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@Component
public class KnowledgeImpl implements Knowledge {

    private final KnowledgeConfig config;
    private final QueryDocumentManager queryDocumentManager;
    private final DashscopeClient dashscope;

    @Override
    public String toString() {
        return "moss://backed/knowledge";
    }

    @Override
    public CompletionStage<MatchResult> matches(String query, MatchOption option) {
        return rewriteQuery(query, option)
                .thenCompose(rwQuery -> {
                    final var limit = option.getLimit();
                    final var distance = option.getDistance();
                    return queryDocumentManager.matches(rwQuery, limit, distance);
                })
                .thenApply(itemDOs -> {
                    final var items = itemDOs.stream()
                            .map(KnowledgeImpl::toMatchResultItem)
                            .toList();
                    return new MatchResult(items);
                })
                .whenComplete((v, ex) -> {
                    if (null != ex) {
                        log.warn("{} matches error!", this, ex);
                    }
                });
    }

    private static MatchResult.Item toMatchResultItem(KnowledgeMatchedItemDO itemDO) {
        return new MatchResult.Item(
                itemDO.getContent(),
                itemDO.getDistance()
        );
    }

    /*
     * 是否启用重写查询
     */
    private boolean isEnableRewriteQuery(String query, MatchOption option) {

        // 如果查询内容超过了一次分块大小，则强制启用重写查询
        if (query.length() > config.getChunk().getSize()) {
            return true;
        }

        // 其他情况则遵循匹配选项的设置
        return option.isRewriteEnabled();

    }

    private CompletionStage<String> rewriteQuery(String query, MatchOption option) {

        if (!isEnableRewriteQuery(query, option)) {
            return completedStage(query);
        }

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)

                // 添加SYSTEM-MESSAGE
                .building(builder -> {
                    try {
                        final var loader = KnowledgeImpl.class.getClassLoader();
                        final var resourceName = "backend/prompt/knowledge-rewrite-query.md";
                        final var prompt = IOUtils.resourceToString(resourceName, StandardCharsets.UTF_8, loader);
                        builder.addMessage(Message.ofSystem(prompt));
                    } catch (IOException ex) {
                        log.warn("{}/rewrite-query fetch prompt failed!", this, ex);
                    }
                })

                // 添加对话上下文
                .building(builder -> {
                    final var messages = option.getHistory();
                    if (null == messages || messages.isEmpty()) {
                        return;
                    }
                    final var history = messages.stream()
                            .filter(message -> isIn(message.role(), USER, AI))
                            .toList();
                    builder.addMessages(history);
                })

                // 本次问题
                .addMessage(Message.ofUser(query))
                .build();
        return dashscope.chat().async(request)
                .thenApply(response -> response.output().best().message().text());
    }

    public static void main(String... args) throws IOException {
        final var content = IOUtils.resourceToString("backend/prompt/knowledge-rewrite-query.md", StandardCharsets.UTF_8);
        System.out.println(content);
    }

}
