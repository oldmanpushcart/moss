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
                            .build();
                    return dashscope.chat().flow(newChatRequest);
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
                .addMessages(request.messages().stream()
                        .filter(message-> CommonUtils.isIn(message.role(), Message.Role.USER, Message.Role.AI))
                        .toList())
                .addMessage(Message.ofSystem("""
                        你是一个智能函数路由助手，负责根据用户的输入上下文选择最合适的函数进行调用。逐行仅返回函数名称列表。
                        函数列表：
                        %s
                        """.formatted(
                        tools.stream()
                                .map(ChatFunctionTool::meta)
                                .map(meta -> "%s: %s".formatted(meta.name(), meta.description()))
                                .reduce((first, second) -> first + "\n" + second)
                                .orElse("")
                )))
                .build();
        return dashscope.chat().async(choiceToolsRequest)
                .thenApply(choiceToolsResponse -> {
                    final var responseText = choiceToolsResponse.output().best().message().text();
                    final var nameSet = splitLines(responseText);
                    return tools.stream()
                            .filter(tool -> nameSet.contains(tool.meta().name()))
                            .toList();
                });
    }

    private static Set<String> splitLines(String string) {
        if (isBlank(string)) {
            return emptySet();
        }
        return Stream.of(StringUtils.split(string, "\r\n"))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }

}
