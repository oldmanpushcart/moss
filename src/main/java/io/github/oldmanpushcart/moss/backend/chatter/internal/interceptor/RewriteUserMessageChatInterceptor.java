package io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.moss.backend.dashscope.util.DashscopeUtils.requireLastUserMessage;
import static io.github.oldmanpushcart.moss.util.FileUtils.probeContentType;
import static java.util.Collections.emptyList;

/**
 * 重写用户输入信息拦截器
 */
@Component
public class RewriteUserMessageChatInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.process(chain.request());
        }

        final var newRequest = ChatRequest.newBuilder(request)
                .messages(requireHistoryMessages(request))
                .addMessage(rewriteLastUserMessage(request))
                .build();
        return chain.process(newRequest);
    }

    /*
     * 提取历史信息
     * 1. 消息列表中下标范围[0,n-1)信息为历史信息
     */
    private List<Message> requireHistoryMessages(ChatRequest request) {
        return request.messages().size() == 1
                ? emptyList()
                : request.messages().subList(0, request.messages().size() - 1);
    }


    /*
     * 重写用户输入消息
     * 1. 用户输入内容中追加附件文件信息
     */
    private Message rewriteLastUserMessage(ChatRequest request) {
        final var lastUserMessage = requireLastUserMessage(request);

        final var context = request.context(Chatter.Context.class);
        if (null == context) {
            return lastUserMessage;
        }

        final var resource = context.getAttachments()
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
                lastUserMessage.text(),
                JacksonUtils.toJson(resource)
        ));
    }

}
