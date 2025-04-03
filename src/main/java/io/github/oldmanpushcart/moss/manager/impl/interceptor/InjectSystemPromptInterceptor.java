package io.github.oldmanpushcart.moss.manager.impl.interceptor;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.manager.MossChatManager;
import io.reactivex.rxjava3.core.Flowable;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 系统提示语注入拦截器
 */
@Component
public class InjectSystemPromptInterceptor implements MossChatInterceptor {

    @Override
    public CompletionStage<Flowable<ChatResponse>> intercept(Chain chain) {
        final var request = chain.request();
        final var newRequest = ChatRequest.newBuilder(request)
                .building(builder -> {
                    try {
                        final var loader = MossChatManager.class.getClassLoader();
                        final var prompt = IOUtils.resourceToString("prompt/system-prompt.md", UTF_8, loader);
                        builder.messages(List.of(Message.ofSystem(prompt)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .addMessages(request.messages())
                .build();
        return chain.process(newRequest);
    }

}
