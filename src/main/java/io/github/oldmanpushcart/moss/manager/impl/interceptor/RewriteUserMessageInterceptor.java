package io.github.oldmanpushcart.moss.manager.impl.interceptor;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.manager.MossChatManager;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.moss.util.FileUtils.probeContentType;
import static java.util.Collections.emptyList;

/**
 * 重写用户输入信息拦截器
 */
@Component
public class RewriteUserMessageInterceptor implements MossChatInterceptor {

    @Override
    public CompletionStage<Flowable<ChatResponse>> intercept(Chain chain) {
        final var request = chain.request();
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
     * 提取最后一条用户输入信息
     * 1. 消息列表中下标为n-1的消息为用户输入消息
     * 2. 如果消息列表中下标为n-1的消息不是用户输入消息，则抛出异常
     */
    private Message requireLastUserMessage(ChatRequest request) {
        final var messages = request.messages();
        final var lastMessage = messages.get(messages.size() - 1);
        if (lastMessage.role() != Message.Role.USER) {
            throw new IllegalArgumentException("Last message not user message!");
        }
        return lastMessage;
    }


    /*
     * 重写用户输入消息
     * 1. 用户输入内容中追加附件文件信息
     */
    private Message rewriteLastUserMessage(ChatRequest request) {
        final var lastUserMessage = requireLastUserMessage(request);

        final var context = request.context(MossChatManager.Context.class);
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
