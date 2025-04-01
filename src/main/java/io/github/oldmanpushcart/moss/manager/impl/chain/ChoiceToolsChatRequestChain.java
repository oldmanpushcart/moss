package io.github.oldmanpushcart.moss.manager.impl.chain;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.moss.util.CommonUtils;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.completedStage;

@Component
public class ChoiceToolsChatRequestChain implements ChatRequestChain {

    private final DashscopeClient dashscope;
    private final Set<ChatFunctionTool> tools;

    @Autowired
    public ChoiceToolsChatRequestChain(DashscopeClient dashscope, Set<ChatFunction<?, ?>> functions) {
        this.dashscope = dashscope;
        this.tools = buildingTools(functions);
    }

    private static Set<ChatFunctionTool> buildingTools(Set<ChatFunction<?, ?>> functions) {
        return functions.stream()
                .map(ChatFunctionTool::of)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public CompletionStage<ChatRequest> chain(ChatRequest request) {
        return completedStage(request)
                .thenCompose(this::choiceTools)
                .thenApply(tools -> {
                    if (tools.isEmpty()) {
                        return request;
                    }
                    return ChatRequest.newBuilder(request)
                            .option(ChatOptions.ENABLE_PARALLEL_TOOL_CALLS, true)
                            .tools(tools)
                            .build();
                });
    }

    private CompletionStage<Collection<? extends Tool>> choiceTools(ChatRequest request) {
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
                    try {
                        final var loader = ChoiceToolsChatRequestChain.class.getClassLoader();
                        final var choiceToolsPrompt = IOUtils
                                .resourceToString("prompt/choice-tools-prompt.md", UTF_8, loader)
                                .formatted(toolsJson);
                        builder.addMessage(Message.ofSystem(choiceToolsPrompt));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

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
