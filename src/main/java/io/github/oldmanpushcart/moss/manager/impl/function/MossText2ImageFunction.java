package io.github.oldmanpushcart.moss.manager.impl.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageResponse;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import io.github.oldmanpushcart.moss.infra.downloader.Downloader;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
@ChatFnName("moss_t2i_fn")
@ChatFnDescription("文生图：根据文本提示生成图片")
public class MossText2ImageFunction
        implements ChatFunction<MossText2ImageFunction.Parameter, MossText2ImageFunction.Result> {

    private final Downloader downloader;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final var request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V2_1_PLUS)
                .option(GenImageOptions.NUMBER, 1)
                .prompt(parameter.prompt())
                .building(builder -> {
                    if (null != parameter.negative()) {
                        builder.negative(parameter.negative());
                    }
                })
                .build();
        return caller.client().image().generation().task(request)
                .thenCompose(half ->
                        half.waitingFor(Task.WaitStrategies.until(
                                Duration.ofSeconds(1),
                                Duration.ofMinutes(1)
                        )))
                .thenApply(response ->
                        response.output().results().stream()
                                .filter(GenImageResponse.Item::isSuccess)
                                .map(GenImageResponse.Item::image)
                                .toList())
                .thenCompose(downloader::downloads)
                .thenApply(Result::new);
    }

    public record Parameter(

            @JsonPropertyDescription("生成图像正向提示，描述生成图片所期待的内容")
            @JsonProperty(required = true)
            String prompt,

            @JsonPropertyDescription("生成图像负向提示，描述生成图片所不期待的内容")
            @JsonProperty
            String negative

    ) {

    }

    public record Result(

            @JsonPropertyDescription("生成图像的URI列表")
            @JsonProperty
            List<URI> imageURIs

    ) {

    }

}
