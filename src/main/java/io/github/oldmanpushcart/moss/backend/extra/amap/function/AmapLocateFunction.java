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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;

@Component
@ChatFnName("amap_locate")
@ChatFnDescription("获取当前IP所在的地理位置信息。当问题可能涉及到当前所在城市、位置时候使用。")
public class AmapLocateFunction implements ChatFunction<AmapLocateFunction.Parameter, AmapLocateFunction.Result> {

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
        final var apiUrlBuilder = requireNonNull(HttpUrl.parse("https://restapi.amap.com/v3/ip"))
                .newBuilder()
                .addQueryParameter("key", config.getApiKey());

        if(StringUtils.isNotBlank(parameter.ip())) {
            apiUrlBuilder.addQueryParameter("ip", parameter.ip());
        }

        final var request = new Request.Builder()
                .url(apiUrlBuilder.build())
                .get()
                .build();
        return OkHttpUtils.async(amapHttp, request)
                .thenCompose(OkHttpUtils::thenComposeStringBody)
                .thenApply(Result::new);
    }

    public record Parameter(
            @JsonPropertyDescription("IP地址；如果不传IP地址，则用当前本机的出口IP地址进行定位")
            @JsonProperty
            String ip
    ) {

    }


    public record Result(

            @JsonProperty
            String output

    ) {

    }

}
