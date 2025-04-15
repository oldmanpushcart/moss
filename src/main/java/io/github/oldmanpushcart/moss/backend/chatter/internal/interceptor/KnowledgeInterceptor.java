package io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.github.oldmanpushcart.moss.backend.knowledge.Knowledge;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.moss.util.DashscopeUtils.*;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class KnowledgeInterceptor implements Interceptor {

    private final Knowledge knowledge;

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 只处理对话请求
        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.process(chain.request());
        }

        // 只处理最后一个消息是用户消息的请求
        if (!isLastMessageFromUser(request)) {
            return chain.process(chain.request());
        }

        // 只处理对话管理器发起的请求
        if (!isCameFromChatter(request)) {
            return chain.process(chain.request());
        }

        // 只处理启用了知识库的请求
        final var context = request.context(Chatter.Context.class);
        if(!context.isKnowledgeEnabled()) {
            return chain.process(chain.request());
        }

        return matches(request)
                .thenCompose(result -> {
                    context.setKnowledgeMatchResult(result);
                    return chain.process(request);
                });
    }

    private CompletionStage<Knowledge.MatchResult> matches(ChatRequest request) {
        final var query = requireLastMessageFromUser(request).text();
        final var option = new Knowledge.MatchOption()
                .history(requireHistoryMessages(request));
        return knowledge.matches(query, option);
    }

}
