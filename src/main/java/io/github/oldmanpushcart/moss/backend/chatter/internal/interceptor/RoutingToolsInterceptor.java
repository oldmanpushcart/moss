package io.github.oldmanpushcart.moss.backend.chatter.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.moss.util.CommonUtils;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.moss.util.DashscopeUtils.isCameFromChatter;
import static io.github.oldmanpushcart.moss.util.DashscopeUtils.isLastMessageFromUser;

/**
 * 路由工具拦截器
 */
@Component
public class RoutingToolsInterceptor implements Interceptor {

    private final Set<ChatFunctionTool> tools;

    @Autowired
    public RoutingToolsInterceptor(Set<ChatFunction<?, ?>> functions) {
        this.tools = buildingTools(functions);
    }

    private static Set<ChatFunctionTool> buildingTools(Set<ChatFunction<?, ?>> functions) {
        return functions.stream()
                .map(ChatFunctionTool::of)
                .collect(Collectors.toUnmodifiableSet());
    }

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

        return routingTools(chain.client(), request)
                .thenApply(tools -> {
                    if (tools.isEmpty()) {
                        return request;
                    }
                    return ChatRequest.newBuilder(request)
                            .context(RoutingToolsInterceptor.class, this)
                            .option(ChatOptions.ENABLE_PARALLEL_TOOL_CALLS, true)
                            .tools(tools)
                            .build();
                })
                .thenCompose(chain::process);
    }

    private CompletionStage<Collection<? extends Tool>> routingTools(DashscopeClient dashscope, ChatRequest request) {
        final var choiceToolsRequest = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .option(ChatOptions.RESPONSE_FORMAT, ChatOptions.ResponseFormat.JSON)
                .building(builder -> {

                    final var toolsMap = tools.stream()
                            .map(ChatFunctionTool::meta)
                            .collect(Collectors.toMap(
                                    ChatFunctionTool.Meta::name,
                                    ChatFunctionTool.Meta::description
                            ));

                    builder.addMessage(Message.ofSystem("""
                            你是一个智能函数路由助手，负责根据用户的输入的意图选择最合适的函数进行调用。
                            
                            ### 行为要求
                            - 需要提取出函数名并组成字符串数组，仅以JSON格式输出，不要MARKDOWN渲染
                            - 严格按照输出格式的要求输出
                            - 严格要求你回答的内容仅限于函数路由，不要输出与函数路由无关内容
                            - 如匹配不到符合意图的函数，则输出为空
                            
                            ### 输出格式
                            ['函数1','函数2‘，’函数3']
                            
                            ### 函数列表
                            %s
                            """.formatted(
                            JacksonUtils.toJson(toolsMap)
                    )));

                })
                .building(builder-> {

                    final var messages = request.messages()
                            .stream()
                            .filter(message-> CommonUtils.isIn(message.role(), Message.Role.USER, Message.Role.AI))
                            .toList();

                    builder.addMessages(messages);

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
