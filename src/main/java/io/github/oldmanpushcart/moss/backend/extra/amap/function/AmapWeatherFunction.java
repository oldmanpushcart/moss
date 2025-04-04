package io.github.oldmanpushcart.moss.backend.extra.amap.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.moss.backend.extra.amap.AmapConfig;
import io.github.oldmanpushcart.moss.util.OkHttpUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;

@Component
@ChatFnName("amap_weather")
@ChatFnDescription("根据目标区域的adcode代码，查询目标区域当前/未来的天气情况")
public class AmapWeatherFunction implements ChatFunction<AmapWeatherFunction.Parameter, AmapWeatherFunction.Result> {

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
        final var apiUrl = requireNonNull(HttpUrl.parse("https://restapi.amap.com/v3/weather/weatherInfo"))
                .newBuilder()
                .addQueryParameter("key", config.getApiKey())
                .addQueryParameter("city", parameter.adcode())
                .addQueryParameter("extensions", "all")
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

            @JsonPropertyDescription("地区的ADCODE代码")
            @JsonProperty
            String adcode

    ) {

    }


    public record Result(

            @JsonProperty
            String output

    ) {

    }

}
