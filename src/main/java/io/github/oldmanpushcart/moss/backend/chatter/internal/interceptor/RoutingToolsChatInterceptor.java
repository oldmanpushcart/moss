package io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.moss.util.CommonUtils;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedStage;

/**
 * 路由工具拦截器
 */
@Component
public class RoutingToolsChatInterceptor implements ChatInterceptor {

    private final Set<ChatFunctionTool> tools;

    @Autowired
    public RoutingToolsChatInterceptor(Set<ChatFunction<?, ?>> functions) {
        this.tools = buildingTools(functions);
    }

    private static Set<ChatFunctionTool> buildingTools(Set<ChatFunction<?, ?>> functions) {
        return functions.stream()
                .map(ChatFunctionTool::of)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> intercept(Chain chain) {
        return completedStage(chain.request())
                .thenCompose(request ->
                        routingTools(chain.dashscope(), request)
                                .thenApply(tools -> {
                                    if (tools.isEmpty()) {
                                        return request;
                                    }
                                    return ChatRequest.newBuilder(request)
                                            .option(ChatOptions.ENABLE_PARALLEL_TOOL_CALLS, true)
                                            .tools(tools)
                                            .build();
                                }))
                .thenCompose(chain::process);
    }

    private CompletionStage<Collection<? extends Tool>> routingTools(DashscopeClient dashscope, ChatRequest request) {
        final var choiceToolsRequest = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .option(ChatOptions.RESPONSE_FORMAT, ChatOptions.ResponseFormat.JSON)
                .addMessages(request.messages().stream()
                        .filter(message -> CommonUtils.isIn(message.role(), Message.Role.USER, Message.Role.AI))
                        .toList())
                .building(builder -> {

                    final var toolsMap = tools.stream()
                            .map(ChatFunctionTool::meta)
                            .collect(Collectors.toMap(
                                    ChatFunctionTool.Meta::name,
                                    ChatFunctionTool.Meta::description
                            ));

                    final var toolsJson = JacksonUtils.toJson(toolsMap);

                    builder.addMessage(Message.ofSystem("""
                            你是一个智能函数路由助手，负责根据用户的输入上下文选择最合适的函数进行调用。
                            
                            ### 行为要求
                            - 需要提取出函数名并组成字符串数组
                            - 请仅输出JSON
                            - 不要以markdown的格式输出
                            - 不要输出其他无关内容
                            
                            ### 输出格式
                            ['函数1','函数2‘，’函数3']
                            
                            ### 函数列表
                            %s
                            """.formatted(toolsJson)));

                })
                .build();
        return dashscope.chat().async(choiceToolsRequest)
                .thenApply(choiceToolsResponse -> {
                    final var responseText = choiceToolsResponse.output().best().message().text();
                    final var fuNameArray = JacksonUtils.toObject(responseText, String[].class);
                    final var fnNameSet = Stream.of(fuNameArray)
                            .filter(fnName -> null != fnName && !fnName.isBlank())
                            .map(String::trim)
                            .collect(Collectors.toUnmodifiableSet());
                    return tools.stream()
                            .filter(tool -> fnNameSet.contains(tool.meta().name()))
                            .toList();
                });
    }

}
