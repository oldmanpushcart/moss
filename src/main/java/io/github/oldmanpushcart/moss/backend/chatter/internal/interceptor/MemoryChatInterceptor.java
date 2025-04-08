package io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.github.oldmanpushcart.moss.backend.memory.Memory;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.moss.backend.dashscope.util.DashscopeUtils.requireLastUserMessage;

/**
 * 对话记忆拦截器
 */
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class MemoryChatInterceptor implements Interceptor {

    private final Memory memory;

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.process(chain.request());
        }

        final var requestMessage = requireLastUserMessage(request);
        final var newRequest = ChatRequest.newBuilder(request)
                .messages(recall(request))
                .addMessages(request.messages())
                .build();

        final var stringBuf = new StringBuilder();
        return chain.process(newRequest)
                .thenApply(v -> {
                    if (!(v instanceof Flowable)) {
                        return v;
                    }
                    @SuppressWarnings("unchecked") final var responseFlow = (Flowable<ChatResponse>) v;
                    return responseFlow
                            .doOnNext(response -> {

                                /*
                                 * 如果不是增量输出，则说明是全量输出
                                 * 需要每次均清空缓冲区
                                 */
                                final var isIncrementalOutput = request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true);
                                if (!isIncrementalOutput) {
                                    stringBuf.setLength(0);
                                }

                                // 将当前输出添加到输出缓存中
                                final var text = response.output().best().message().text();
                                stringBuf.append(text);

                            })
                            .doOnComplete(() -> {
                                final var fragment = new Memory.Fragment() {{
                                    requestMessage(requestMessage);
                                    responseMessage(Message.ofAi(stringBuf.toString()));
                                }};
                                memory.saveOrUpdate(fragment);
                            });
                });
    }

    private List<Message> recall(ChatRequest request) {
        final var context = request.context(Chatter.Context.class);
        final var fragments = null != context && null != context.getTimeline()
                ? memory.recall(context.getTimeline())
                : memory.recall();
        return fragments.stream()
                .flatMap(f -> Stream.of(f.requestMessage(), f.responseMessage()))
                .toList();
    }

}
