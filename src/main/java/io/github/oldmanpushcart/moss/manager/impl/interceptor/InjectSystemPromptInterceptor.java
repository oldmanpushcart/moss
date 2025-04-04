package io.github.oldmanpushcart.moss.manager.impl.interceptor;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.MossConfig;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 系统提示语注入拦截器
 */
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class InjectSystemPromptInterceptor implements MossChatInterceptor {

    private final MossConfig config;

    @Override
    public CompletionStage<Flowable<ChatResponse>> intercept(Chain chain) {
        final var request = chain.request();

        /*
         * 检查系统提示语文件是否存在且有效
         */
        final var systemPromptLocation = config.getSystemPromptLocation();
        if (null == systemPromptLocation
            || !systemPromptLocation.toFile().exists()
            || !systemPromptLocation.toFile().canRead()
            || !systemPromptLocation.toFile().isFile()) {
            return chain.process(request);
        }

        final var newRequest = ChatRequest.newBuilder(request)
                .building(builder -> {
                    try {
                        final var prompt = Files.readString(systemPromptLocation, UTF_8);
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
