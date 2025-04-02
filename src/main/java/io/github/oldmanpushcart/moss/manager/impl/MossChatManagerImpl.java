package io.github.oldmanpushcart.moss.manager.impl;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.manager.MossChatContext;
import io.github.oldmanpushcart.moss.manager.MossChatManager;
import io.github.oldmanpushcart.moss.manager.impl.chain.ChoiceToolsChatRequestChain;
import io.github.oldmanpushcart.moss.manager.impl.chain.MemoryRecallChatRequestChain;
import io.github.oldmanpushcart.moss.manager.impl.chain.MossPromptChatRequestChain;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.moss.util.FileUtils.probeContentType;
import static java.util.concurrent.CompletableFuture.completedStage;

@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class MossChatManagerImpl implements MossChatManager {

    private final DashscopeClient dashscope;
    private final ChoiceToolsChatRequestChain choiceToolsChatRequestChain;
    private final MemoryRecallChatRequestChain memoryRecallChatRequestChain;
    private final MossPromptChatRequestChain mossPromptChatRequestChain;

    @Override
    public CompletionStage<Flowable<ChatResponse>> chat(MossChatContext context) {

        final var request = ChatRequest.newBuilder()
                .context(MossChatContext.class, context)
                .model(ChatModel.QWQ_PLUS)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption()
                        .forcedSearch(false)
                        .enableSource(true)
                        .searchStrategy(ChatSearchOption.SearchStrategy.STANDARD)
                )
                .addMessage(rewriteUserMessage(context))
                .build();

        return completedStage(request)
                .thenCompose(memoryRecallChatRequestChain::chain)
                .thenCompose(choiceToolsChatRequestChain::chain)
                .thenCompose(mossPromptChatRequestChain::chain)
                .thenCompose(r -> dashscope.chat().flow(r))
                .whenComplete((v, ex) -> {
                    if (null != ex) {
                        log.warn("moss://chat/flow error!", ex);
                    }
                });
    }

    private Message rewriteUserMessage(MossChatContext context) {
        final var resource = context.attachments()
                .stream()
                .filter(file -> file.exists() && file.canRead() && file.isFile())
                .map(file ->
                        new HashMap<String, Object>() {{
                            put("mime", probeContentType(file));
                            put("uri", file.toURI().toASCIIString());
                        }})
                .toList();
        return Message.ofUser("""
                用户输入：
                %s
                
                参考资料：
                %s
                """.formatted(
                context.fragment().requestMessage().text(),
                JacksonUtils.toJson(resource)
        ));
    }

}
