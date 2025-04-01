package io.github.oldmanpushcart.moss.manager.impl;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import io.github.oldmanpushcart.moss.infra.memory.MemoryFragment;
import io.github.oldmanpushcart.moss.manager.ChatManager;
import io.github.oldmanpushcart.moss.util.CommonUtils;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.moss.util.FileUtils.probeContentType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class ChatManagerImpl implements ChatManager {

    private final DashscopeClient dashscope;
    private final Memory memory;
    private final Set<ChatFunctionTool> tools;

    @Autowired
    public ChatManagerImpl(DashscopeClient dashscope, Memory memory, Set<ChatFunction<?, ?>> functions) {
        this.dashscope = dashscope;
        this.memory = memory;
        this.tools = buildingTools(functions);
        log.debug("moss://chat/function loaded: {}", tools);
    }

    private static Set<ChatFunctionTool> buildingTools(Set<ChatFunction<?, ?>> functions) {
        return functions.stream()
                .map(ChatFunctionTool::of)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> chat(MemoryFragment fragment, List<File> attachments) {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_MAX)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption()
                        .forcedSearch(true)
                        .enableSource(true)
                        .searchStrategy(ChatSearchOption.SearchStrategy.STANDARD)
                )
                .building(builder -> {
                    try {
                        final var prompt = IOUtils.resourceToString("prompt.md", UTF_8, ChatManager.class.getClassLoader());
                        builder.addMessage(Message.ofSystem(prompt));
                    } catch (IOException e) {
                        // ignore
                    }
                })
                .addMessages(memory.recall(fragment.fragmentId()).stream()
                        .flatMap(f -> Stream.of(f.requestMessage(), f.responseMessage()))
                        .toList())
                .addMessage(rewriteUserMessage(fragment.requestMessage(), attachments))
                .build();

        return CompletableFuture.completedStage(request)
                .thenCompose(r -> choiceTools(dashscope, r))
                .thenCompose(choices -> {
                    final var newChatRequest = ChatRequest.newBuilder(request)
                            .addTools(choices)
                            .option(ChatOptions.ENABLE_PARALLEL_TOOL_CALLS, true)
                            .build();
                    return dashscope.chat().flow(newChatRequest);
                })
                .whenComplete((v, ex) -> {
                    if (null != ex) {
                        log.warn("moss://chat/flow error!", ex);
                    }
                });
    }

    private Message rewriteUserMessage(Message userMessage, List<File> attachments) {
        final var resource = attachments.stream()
                .filter(file -> file.exists() && file.canRead() && file.isFile())
                .map(file ->
                        new HashMap<String, Object>() {{
                            put("mime", probeContentType(file));
                            put("uri", file.toURI());
                        }})
                .toList();
        return Message.ofUser("""
                用户输入：
                %s
                
                参考资料：
                %s
                """.formatted(
                userMessage.text(),
                JacksonUtils.toJson(resource)
        ));
    }

    private CompletionStage<Collection<? extends Tool>> choiceTools(DashscopeClient dashscope, ChatRequest request) {
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
                            - 请仅输出JSON字符串
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
                            .filter(StringUtils::isNotBlank)
                            .map(String::trim)
                            .collect(Collectors.toUnmodifiableSet());
                    return tools.stream()
                            .filter(tool -> fnNameSet.contains(tool.meta().name()))
                            .toList();
                });
    }

}
