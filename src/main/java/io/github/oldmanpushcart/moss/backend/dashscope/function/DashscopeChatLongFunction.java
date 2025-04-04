package io.github.oldmanpushcart.moss.backend.dashscope.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.moss.backend.uploader.Uploader;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
@ChatFnName("dashscope_chat_long")
@ChatFnDescription("文档识别：按照提示要求对文档进行识别")
public class DashscopeChatLongFunction
        implements ChatFunction<DashscopeChatLongFunction.Parameter, DashscopeChatLongFunction.Result> {

    private final Uploader uploader;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> upload(parameter.documentURIs()))
                .thenCompose(resourceUploads -> {
                    final var contents = new ArrayList<Content<?>>() {{
                        add(Content.ofText(parameter.prompt()));
                        addAll(resourceUploads);
                    }};
                    final var request = ChatRequest.newBuilder()
                            .model(ChatModel.QWEN_LONG)
                            .addMessage(Message.ofUser(contents))
                            .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                            .build();

                    return caller.client().chat().directFlow(request)
                            .reduce(new StringBuilder(), (stringBuf, response) -> {
                                stringBuf.append(response.output().best().message().text());
                                return stringBuf;
                            })
                            .toCompletionStage()
                            .thenApply(StringBuilder::toString)
                            .thenApply(Result::new);
                });
    }

    private CompletionStage<List<Content<URI>>> upload(List<URI> resources) {
        CompletionStage<List<Content<URI>>> stage = CompletableFuture.completedStage(new ArrayList<>());
        for (URI resource : resources) {
            stage = stage.thenCompose(list ->
                    uploader.upload(ChatModel.QWEN_LONG, resource)
                            .thenApply(entry -> {
                                final var content = Content.ofFile(entry.upload());
                                list.add(content);
                                return list;
                            }));
        }
        return stage;
    }

    public record Parameter(

            @JsonPropertyDescription("提示词")
            @JsonProperty(required = true)
            String prompt,

            @JsonPropertyDescription("文档URI列表")
            @JsonProperty(required = true)
            List<URI> documentURIs

    ) {

    }

    public record Result(

            @JsonPropertyDescription("识别结果")
            @JsonProperty
            String text

    ) {

    }

}
