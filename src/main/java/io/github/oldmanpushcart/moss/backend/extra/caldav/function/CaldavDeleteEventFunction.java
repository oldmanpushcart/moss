package io.github.oldmanpushcart.moss.backend.extra.caldav.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.moss.backend.extra.caldav.CaldavConfig;
import io.github.oldmanpushcart.moss.util.OkHttpUtils;
import lombok.Value;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;

@Component
@ChatFnName("caldav_delete_event")
@ChatFnDescription("删除日历事件")
public class CaldavDeleteEventFunction implements ChatFunction<CaldavDeleteEventFunction.Parameter, CaldavDeleteEventFunction.Result> {

    @Autowired
    private CaldavConfig config;

    @Autowired
    @Qualifier("caldavHttp")
    private OkHttpClient caldavHttp;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {

        final var href = "%s/%s".formatted(config.getLocation(), parameter.uuid());

        final var request = new Request.Builder()
                .url(config.home().resolve(href).toString())
                .delete()
                .build();

        return OkHttpUtils.async(caldavHttp, request)
                .thenCompose(OkHttpUtils::thenComposeStringBody)
                .thenApply(Result::new);
    }

    public record Parameter(

            @JsonPropertyDescription("日历事件UUID")
            @JsonProperty(required = true)
            String uuid

    ) {

    }

    @Value
    public static class Result {

        @JsonProperty
        String output;

        @JsonProperty
        String prompt = "返回的内容是CALDAV格式的删除结果";

    }

}
