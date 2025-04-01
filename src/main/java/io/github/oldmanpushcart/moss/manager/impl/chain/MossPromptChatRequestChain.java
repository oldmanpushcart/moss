package io.github.oldmanpushcart.moss.manager.impl.chain;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.manager.MossChatManager;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.completedStage;

@Component
public class MossPromptChatRequestChain implements ChatRequestChain {

    @Override
    public CompletionStage<ChatRequest> chain(ChatRequest request) {
        final var newRequest = ChatRequest.newBuilder(request)
                .building(builder -> {
                    try {
                        final var loader = MossChatManager.class.getClassLoader();
                        final var prompt = IOUtils.resourceToString("prompt/moss-prompt.md", UTF_8, loader);
                        builder.messages(List.of(Message.ofSystem(prompt)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .addMessages(request.messages())
                .build();
        return completedStage(newRequest);
    }

}
