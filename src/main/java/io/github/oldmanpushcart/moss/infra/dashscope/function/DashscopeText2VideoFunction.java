package io.github.oldmanpushcart.moss.infra.dashscope.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.TextGenVideoModel;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.TextGenVideoOptions;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.TextGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import io.github.oldmanpushcart.moss.infra.downloader.Downloader;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
@ChatFnName("dashscope_text2video")
@ChatFnDescription("文生视频：根据文本提示生成视频")
public class DashscopeText2VideoFunction
        implements ChatFunction<DashscopeText2VideoFunction.Parameter, DashscopeText2VideoFunction.Result> {

    private final Downloader downloader;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final var http = caller.client().base().http();
        final var request = TextGenVideoRequest.newBuilder()
                .model(TextGenVideoModel.WANX_V2_1_T2V_TURBO)
                .option(TextGenVideoOptions.ENABLE_PROMPT_EXTEND, true)
                .prompt(parameter.prompt())
                .build();
        return caller.client().video().genByText()
                .task(request)
                .thenCompose(half ->
                        half.waitingFor(Task.WaitStrategies.until(
                                Duration.ofMinutes(1),
                                Duration.ofMinutes(15)
                        )))
                .thenCompose(response ->
                        downloader.download(http, response.output().video()))
                .thenApply(Result::new);
    }

    public record Parameter(

            @JsonPropertyDescription("生成视频提示，描述生成视频所期待的内容")
            @JsonProperty(required = true)
            String prompt

    ) {

    }

    public record Result(

            @JsonPropertyDescription("生成视频的URI")
            @JsonProperty
            URI videoURI

    ) {

    }

}
