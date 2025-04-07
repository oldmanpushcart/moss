package io.github.oldmanpushcart.moss.backend.extra.caldav.function;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.moss.backend.extra.caldav.CaldavConfig;
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
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.moss.backend.extra.caldav.util.CaldavUtils.formatDate;

@Component
@ChatFnName("caldav_query_events")
@ChatFnDescription("查询日历事件：根据时间范围查询日历事件，当用户询问一段时间范围内的会议时候使用。")
public class CaldavQueryEventFunction implements ChatFunction<CaldavQueryEventFunction.Parameter, CaldavQueryEventFunction.Result> {

    @Autowired
    private CaldavConfig config;

    @Autowired
    @Qualifier("caldavHttp")
    private OkHttpClient caldavHttp;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {

        final var reportBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <c:filter>
                    <c:comp-filter name="VCALENDAR">
                      <c:comp-filter name="VEVENT">
                        <c:time-range start="%s" end="%s"/>
                      </c:comp-filter>
                    </c:comp-filter>
                  </c:filter>
                  <d:prop>
                    <d:getetag/>
                    <c:calendar-data/>
                  </d:prop>
                </c:calendar-query>
                """
                .formatted(
                        formatDate(parameter.begin()),
                        formatDate(parameter.end())
                );

        final var requestBody = RequestBody.create(reportBody, MediaType.get("text/xml;charset=utf-8"));

        final var request = new Request.Builder()
                .url(config.home().toString())
                .header("Content-Type", "application/xml; charset=utf-8")
                .header("Depth", "1")
                .method("REPORT", requestBody)
                .build();

        return OkHttpUtils.async(caldavHttp, request)
                .thenCompose(OkHttpUtils::thenComposeStringBody)
                .thenApply(Result::new);
    }

    public record Parameter(

            @JsonPropertyDescription("起始时间")
            @JsonProperty(required = true)
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
            Date begin,

            @JsonPropertyDescription("结束时间")
            @JsonProperty(required = true)
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
            Date end

    ) {

    }

    @Value
    public static class Result {

        @JsonProperty
        String output;

        @JsonProperty
        String prompt = "返回的内容是CALDAV格式的查询结果，后续处理中你需要带上会议的UUID";

    }

}
