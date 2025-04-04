package io.github.oldmanpushcart.moss.infra.extra.amap.function;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.moss.infra.extra.amap.AmapConfig;
import io.github.oldmanpushcart.moss.infra.extra.amap.Location;
import io.github.oldmanpushcart.moss.util.OkHttpUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;

/**
 * 高德出行线路规划
 * <p>
 * <a href="https://lbs.amap.com/api/webservice/guide/api/newroute">出行线路规划</a>
 * </p>
 */
@Component
@ChatFnName("amap_direction")
@ChatFnDescription("出行规划可以根据起始地的经纬度和目的地的经纬度信息以及选择的出行方式，规划出可行的出行线路。一共有四种出行方式线路规划：步行、公交、骑行、电动车")
public class AmapDirectionFunction implements ChatFunction<AmapDirectionFunction.Parameter, AmapDirectionFunction.Result> {

    @Autowired
    private AmapConfig config;

    @Autowired
    @Qualifier("amapHttp")
    private OkHttpClient amapHttp;

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final var apiUrl = requireNonNull(HttpUrl.parse("https://restapi.amap.com/v5/direction/"+parameter.mode().toString().toLowerCase()))
                .newBuilder()
                .addQueryParameter("key", config.getApiKey())
                .addQueryParameter("origin", parameter.origin().toString())
                .addQueryParameter("destination", parameter.destination().toString())
                .build();
        final var request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();
        return OkHttpUtils.async(amapHttp, request)
                .thenCompose(OkHttpUtils::thenComposeStringBody)
                .thenApply(Result::new);
    }

    public record Parameter(

            @JsonPropertyDescription("起点经纬度坐标")
            @JsonProperty(required = true)
            Location origin,

            @JsonPropertyDescription("终点经纬度坐标")
            @JsonProperty(required = true)
            Location destination,

            @JsonPropertyDescription("出行方式")
            @JsonProperty(required = true)
            TravelMode mode

    ) {
    }

    public record Result(
            String output
    ) {
    }

    /**
     * 出行方式
     */
    @JsonClassDescription("出行方式")
    public enum TravelMode {

        @JsonPropertyDescription("驾车")
        @JsonProperty
        DRIVING,

        @JsonPropertyDescription("步行")
        @JsonProperty
        WALKING,

        @JsonPropertyDescription("自行车")
        @JsonProperty
        BICYCLING,

        @JsonPropertyDescription("电动车")
        @JsonProperty
        ELECTROBIKE,

        @JsonPropertyDescription("公共交通")
        @JsonProperty
        INTEGRATED

    }

}
