package io.github.oldmanpushcart.moss.manager.impl.function.dashscope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.ImageGenVideoModel;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.ImageGenVideoOptions;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.ImageGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import io.github.oldmanpushcart.moss.infra.downloader.Downloader;
import io.github.oldmanpushcart.moss.infra.uploader.UploadEntry;
import io.github.oldmanpushcart.moss.infra.uploader.Uploader;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
@ChatFnName("dashscope@image2video_fn")
@ChatFnDescription("图生视频：根据参考图片和文本提示生成视频")
public class DashscopeImage2VideoFunction
        implements ChatFunction<DashscopeImage2VideoFunction.Parameter, DashscopeImage2VideoFunction.Result> {

    private final Uploader uploader;
    private final Downloader downloader;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> upload(parameter.referenceImage()))
                .thenCompose(upload -> {
                    final var request = ImageGenVideoRequest.newBuilder()
                            .model(ImageGenVideoModel.WANX_V2_1_I2V_TURBO)
                            .option(ImageGenVideoOptions.ENABLE_PROMPT_EXTEND, true)
                            .prompt(parameter.prompt())
                            .image(upload)
                            .build();
                    return caller.client().video().genByImage()
                            .task(request)
                            .thenCompose(half ->
                                    half.waitingFor(Task.WaitStrategies.until(
                                            Duration.ofMinutes(1),
                                            Duration.ofMinutes(15)
                                    )))
                            .thenCompose(response -> {
                                return downloader.download(response.output().video());
                            })
                            .thenApply(Result::new);
                });
    }

    private CompletionStage<URI> upload(URI resource) {
        return uploader.upload(GenImageModel.WANX_V1, resource)
                .thenApply(UploadEntry::upload);
    }

    public record Parameter(

            @JsonPropertyDescription("参考图像的URI")
            @JsonProperty(required = true)
            URI referenceImage,

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
