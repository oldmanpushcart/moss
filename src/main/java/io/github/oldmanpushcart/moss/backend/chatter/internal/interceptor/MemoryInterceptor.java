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

import static io.github.oldmanpushcart.moss.util.DashscopeUtils.*;

/**
 * 对话记忆拦截器
 */
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class MemoryInterceptor implements Interceptor {

    private final Memory memory;

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

        /*
         * 构建新的对话请求
         * 新的对话请求将会包含历史对话消息
         */
        final var newRequest = ChatRequest.newBuilder(request)
                .messages(recall(request))
                .addMessages(request.messages())
                .build();

        // 用带着记忆的请求处理对话
        return chain.process(newRequest)
                .thenApply(v -> {

                    // async 返回
                    if (v instanceof ChatResponse response) {
                        return processChatResponse(newRequest, response);
                    }

                    // flow 返回
                    else if (v instanceof Flowable<?> flow) {
                        @SuppressWarnings("unchecked") final var responseFlow = (Flowable<ChatResponse>) flow;
                        return processFlowChatResponse(newRequest, responseFlow);
                    }

                    // 其他情况不明，直接原样返回
                    else {
                        return v;
                    }

                });
    }

    // 回忆对话历史记录
    private List<Message> recall(ChatRequest request) {
        final var context = request.context(Chatter.Context.class);
        final var fragments = null != context.getTimeline()
                ? memory.recall(context.getTimeline())
                : memory.recall();
        return fragments.stream()
                .flatMap(f -> Stream.of(f.requestMessage(), f.responseMessage()))
                .toList();
    }

    private ChatResponse processChatResponse(ChatRequest request, ChatResponse response) {
        saveOrUpdateMemoryFragment(request, response.output().best().message().text());
        return response;
    }

    private Flowable<ChatResponse> processFlowChatResponse(ChatRequest request, Flowable<ChatResponse> responseFlow) {

        /*
         * 应答流式输出内容缓存
         * 所以这里需要一个字符串缓存来存储流式输出内容
         */
        final var stringBuf = new StringBuilder();

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

                // 成功完成时触发记忆片段刷新
                .doOnComplete(() -> saveOrUpdateMemoryFragment(request, stringBuf.toString()));

    }

    // 刷新记忆片段
    private void saveOrUpdateMemoryFragment(ChatRequest request, String responseBody) {

        /*
         * 从对话上下文和对话请求、应答构建记忆片段
         */
        final var context = request.context(Chatter.Context.class);
        final var fragment = new Memory.Fragment() {{
            fragmentId(context.getTimeline());
            requestMessage(requireLastMessageFromUser(request));
            responseMessage(Message.ofAi(responseBody));
        }};

        /*
         * 创建或更新记忆片段
         * 此时会回设fragmentId
         */
        memory.saveOrUpdate(fragment);

        /*
         * 设置上下文时间线
         * 这里是一个补偿设置，如果是创建此时fragment中才有id被赋值
         */
        context.setTimeline(fragment.fragmentId());

    }

}
