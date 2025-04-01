package io.github.oldmanpushcart.moss.infra.dashscope.function;

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
import io.github.oldmanpushcart.moss.infra.uploader.UploadEntry;
import io.github.oldmanpushcart.moss.infra.uploader.Uploader;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
@ChatFnName("dashscope_image2image")
@ChatFnDescription("图生图：根据参考图片和文本提示生成图片")
public class DashscopeImage2ImageFunction
        implements ChatFunction<DashscopeImage2ImageFunction.Parameter, DashscopeImage2ImageFunction.Result> {

    private final Uploader uploader;
    private final Downloader downloader;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> upload(parameter.referenceImage()))
                .thenCompose(upload -> {
                    final var request = GenImageRequest.newBuilder()
                            .model(GenImageModel.WANX_V1)
                            .option(GenImageOptions.NUMBER, 1)
                            .reference(upload)
                            .prompt(parameter.prompt())
                            .building(builder -> {
                                if (null != parameter.negative()) {
                                    builder.negative(parameter.negative());
                                }
                                if(null != parameter.refMode()) {
                                    builder.option(GenImageOptions.REF_MODE, parameter.refMode());
                                }
                                if(null != parameter.refStrength()) {
                                    builder.option(GenImageOptions.REF_STRENGTH, parameter.refStrength());
                                }
                            })
                            .build();
                    return caller.client().image().generation().task(request)
                            .thenCompose(half ->
                                    half.waitingFor(Task.WaitStrategies.until(
                                            Duration.ofSeconds(1),
                                            Duration.ofMinutes(5)
                                    )))
                            .thenApply(response ->
                                    response.output().results().stream()
                                            .filter(GenImageResponse.Item::isSuccess)
                                            .map(GenImageResponse.Item::image)
                                            .toList())
                            .thenCompose(imageURIs-> downloader.downloads(caller.client().base().http(), imageURIs))
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

            @JsonPropertyDescription("生成图像正向提示，描述生成图片所期待的内容")
            @JsonProperty(required = true)
            String prompt,

            @JsonPropertyDescription("生成图像负向提示，描述生成图片所不期待的内容")
            @JsonProperty
            String negative,

            @JsonPropertyDescription("参考图像的匹配模式")
            @JsonProperty
            GenImageOptions.RefMode refMode,

            @JsonPropertyDescription("""
                    参考图像的匹配强度
                    取值范围为[0.0, 1.0]。取值越大，代表生成的图像与参考图越相似。
                    """)
            @JsonProperty
            Float refStrength

    ) {

    }

    public record Result(

            @JsonPropertyDescription("生成图像的URI列表")
            @JsonProperty
            List<URI> imageURIs

    ) {

    }

}
