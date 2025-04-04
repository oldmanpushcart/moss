package io.github.oldmanpushcart.moss.backend.extra.amap.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.moss.backend.extra.amap.AmapConfig;
import io.github.oldmanpushcart.moss.backend.extra.amap.Location;
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
 * 高德逆地址编码
 * <p>
 * <a href="https://lbs.amap.com/api/webservice/guide/api/georegeo#t5">高德逆地理编码</a>
 * </p>
 */
@Component
@ChatFnName("amap_geocode_regeo")
@ChatFnDescription("逆地理编码：经纬度转换为地址。例如：116.480881,39.989410：北京市朝阳区阜通东大街6号")
public class AmapGeocodeRegeoFunction implements ChatFunction<AmapGeocodeRegeoFunction.Parameter, AmapGeocodeRegeoFunction.Result> {

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
        final var apiUrl = requireNonNull(HttpUrl.parse("https://restapi.amap.com/v3/geocode/regeo"))
                .newBuilder()
                .addQueryParameter("key", config.getApiKey())
                .addQueryParameter("location", parameter.location().toString())
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

            @JsonPropertyDescription("经纬度")
            @JsonProperty(required = true)
            Location location

    ) {

    }

    public record Result(

            @JsonProperty
            String output

    ) {

    }

}
