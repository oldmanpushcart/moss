package io.github.oldmanpushcart.moss.manager.impl.chain;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import io.github.oldmanpushcart.moss.manager.MossChatContext;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedStage;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class MemoryRecallChatRequestChain implements ChatRequestChain {

    private final Memory memory;

    @Override
    public CompletionStage<ChatRequest> chain(ChatRequest request) {

        /*
         * 如果没有Moss上下文则说明不是Moss请求
         */
        final var context = request.context(MossChatContext.class);
        if (null == context
            || null == context.fragment()) {
            return completedStage(request);
        }

        /*
         * 对Moss请求进行记忆召唤
         */
        final var newRequest = ChatRequest.newBuilder(request)
                .building(builder -> {
                    final var messages = memory.recall(context.fragment().fragmentId())
                            .stream()
                            .flatMap(f -> Stream.of(f.requestMessage(), f.responseMessage()))
                            .toList();
                    builder.messages(messages);
                })
                .addMessages(request.messages())
                .build();
        return completedStage(newRequest);
    }

}
