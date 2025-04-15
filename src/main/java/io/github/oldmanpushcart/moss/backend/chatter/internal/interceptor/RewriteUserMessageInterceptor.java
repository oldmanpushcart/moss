package io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.github.oldmanpushcart.moss.backend.knowledge.Knowledge;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.moss.util.DashscopeUtils.*;
import static io.github.oldmanpushcart.moss.util.FileUtils.probeContentType;

/**
 * 重写用户输入信息拦截器
 */
@Component
public class RewriteUserMessageInterceptor implements Interceptor {

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

        final var newRequest = ChatRequest.newBuilder(request)
                .context(RewriteUserMessageInterceptor.class, this)
                .messages(requireHistoryMessages(request))
                .addMessage(rewriteLastUserMessage(request))
                .build();
        return chain.process(newRequest);
    }


    /*
     * 重写用户输入消息
     * 1. 用户输入内容中追加附件文件信息
     */
    private Message rewriteLastUserMessage(ChatRequest request) {

        final var lastUserMessage = requireLastMessageFromUser(request);
        final var context = request.context(Chatter.Context.class);
        final var stringBuf = new StringBuilder();

        if (context.isKnowledgeEnabled()) {
            final var knowledgeMatchItems = context.getKnowledgeMatchResult()
                    .getItems()
                    .stream()
                    .map(Knowledge.MatchResult.Item::getContent)
                    .toList();
            stringBuf.append("""
                    参考资料：
                    %s
                    
                    """.formatted(
                    JacksonUtils.toJson(knowledgeMatchItems)
            ));
        }

        if (context.isAttachmentsEnabled()) {
            final var attachments = context.getAttachments()
                    .stream()
                    .filter(file -> file.exists() && file.canRead() && file.isFile())
                    .map(file ->
                            new HashMap<String, Object>() {{
                                put("mime", probeContentType(file));
                                put("uri", file.toURI().toASCIIString());
                            }})
                    .toList();
            stringBuf.append("""
                    输入附件：
                    %s
                    
                    """.formatted(
                    JacksonUtils.toJson(attachments)
            ));
        }

        stringBuf.append("""
                用户输入：
                %s
                """.formatted(lastUserMessage.text()));

        return Message.ofUser(stringBuf.toString());
    }

}
