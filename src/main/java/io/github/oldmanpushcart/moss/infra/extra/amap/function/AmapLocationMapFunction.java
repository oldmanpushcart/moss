package io.github.oldmanpushcart.moss.infra.extra.amap.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.moss.infra.downloader.Downloader;
import io.github.oldmanpushcart.moss.infra.extra.amap.AmapConfig;
import io.github.oldmanpushcart.moss.infra.extra.amap.Location;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;

/**
 * 高德静态地图：根据地图中心点展示地图
 * <p>
 * <a href="https://lbs.amap.com/api/webservice/guide/api/staticmaps">奥德静态地图</a>
 * </p>
 */
@Component
@ChatFnName("amap_location_map")
@ChatFnDescription("根据经纬度获取地图点位展示图片")
public class AmapLocationMapFunction implements ChatFunction<AmapLocationMapFunction.Parameter, AmapLocationMapFunction.Result> {

    @Autowired
    private AmapConfig config;

    @Autowired
    @Qualifier("amapHttp")
    private OkHttpClient amapHttp;

    @Autowired
    private Downloader downloader;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final var filename = "amap-staticmap-%s.png".formatted(UUID.randomUUID());
        final var apiUrl = requireNonNull(HttpUrl.parse("https://restapi.amap.com/v3/staticmap"))
                .newBuilder()
                .addQueryParameter("key", config.getApiKey())
                .addQueryParameter("location", parameter.location().toString())
                .addQueryParameter("zoom", String.valueOf(parameter.zoom()))
                .build();
        return downloader.download(amapHttp, apiUrl.uri(), filename)
                .thenApply(Result::new);
    }

    public record Parameter(

            @JsonPropertyDescription("地图中心点")
            @JsonProperty(required = true)
            Location location,

            @JsonPropertyDescription("""
                    地图缩放级别
                    取值范围：[1,17]，数字越大地图越大
                    """)
            @JsonProperty(required = true)
            Integer zoom

    ) {

    }

    public record Result(

            @JsonProperty(required = true)
            URI imageURI

    ) {

    }

}
