package io.github.oldmanpushcart.moss.backend.extra.caldav.function;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.moss.backend.extra.caldav.CaldavConfig;
import io.github.oldmanpushcart.moss.backend.extra.caldav.util.CaldavUtils;
import io.github.oldmanpushcart.moss.util.OkHttpUtils;
import lombok.Value;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Component
@ChatFnName("caldav_create_event")
@ChatFnDescription("创建日历事件")
public class CaldavCreateEventFunction implements ChatFunction<CaldavCreateEventFunction.Parameter, CaldavCreateEventFunction.Result> {

    @Autowired
    private CaldavConfig config;

    @Autowired
    @Qualifier("caldavHttp")
    private OkHttpClient caldavHttp;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {

        final var uuid = UUID.randomUUID().toString().replace("-","");
        final var putBody = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Example Corp//NONSGML Example Calendar//EN
                BEGIN:VEVENT
                UID:%s
                DTSTAMP:%s
                DTSTART:%s
                DTEND:%s
                SUMMARY:%s
                DESCRIPTION:%s
                LOCATION:%s
                STATUS:CONFIRMED
                SEQUENCE:0
                TRANSP:OPAQUE
                END:VEVENT
                END:VCALENDAR
                """
                .formatted(
                        uuid,
                        CaldavUtils.formatDate(new Date()),
                        CaldavUtils.formatDate(parameter.begin()),
                        CaldavUtils.formatDate(parameter.end()),
                        parameter.summary(),
                        parameter.description(),
                        parameter.location()
                );

        final var requestBody = RequestBody.create(putBody, MediaType.get("text/calendar; charset=utf-8"));
        final var href = "%s/%s".formatted(config.getLocation(), uuid);

        final var request = new Request.Builder()
                .url(config.home().resolve(href).toString())
                .header("Content-Type", "text/calendar; charset=utf-8")
                .put(requestBody)
                .build();

        return OkHttpUtils.async(caldavHttp, request)
                .thenCompose(OkHttpUtils::thenComposeStringBody)
                .thenApply(responseBody -> new Result(uuid, responseBody));
    }

    public record Parameter(

            @JsonPropertyDescription("起始时间")
            @JsonProperty(required = true)
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
            Date begin,

            @JsonPropertyDescription("结束时间")
            @JsonProperty(required = true)
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
            Date end,

            @JsonPropertyDescription("会议标题")
            @JsonProperty(required = true)
            String summary,

            @JsonPropertyDescription("会议描述")
            @JsonProperty(required = true)
            String description,

            @JsonPropertyDescription("会议地点")
            @JsonProperty
            String location

    ) {

    }

    @Value
    public static class Result {

        @JsonProperty
        String uuid;

        @JsonProperty
        String output;

        @JsonProperty
        String prompt = "返回的内容是CALDAV格式的创建结果，后续处理中你需要带上会议的UUID";

    }

}
