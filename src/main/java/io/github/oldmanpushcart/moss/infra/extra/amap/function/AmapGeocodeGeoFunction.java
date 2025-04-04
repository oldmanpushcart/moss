package io.github.oldmanpushcart.moss.infra.extra.amap.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.moss.infra.extra.amap.AmapConfig;
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
 * <a href="https://lbs.amap.com/api/webservice/guide/api/georegeo#t4">高德地理编码</a>
 * </p>
 */
@Component
@ChatFnName("amap_geocode_geo")
@ChatFnDescription("地理编码：地址转换为经纬度坐标。例如：北京市朝阳区阜通东大街6号：116.480881,39.989410、天安门：116.397499,39.908722")
public class AmapGeocodeGeoFunction implements ChatFunction<AmapGeocodeGeoFunction.Parameter, AmapGeocodeGeoFunction.Result> {

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
        final var apiUrl = requireNonNull(HttpUrl.parse("https://restapi.amap.com/v3/geocode/geo"))
                .newBuilder()
                .addQueryParameter("key", config.getApiKey())
                .addQueryParameter("address", parameter.address())
                .addQueryParameter("city", parameter.city())
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

            @JsonPropertyDescription("指定查询的地址")
            @JsonProperty(required = true)
            String address,

            @JsonPropertyDescription("指定查询的城市")
            @JsonProperty
            String city

    ) {

    }

    public record Result(

            @JsonProperty
            String output

    ) {

    }

}
