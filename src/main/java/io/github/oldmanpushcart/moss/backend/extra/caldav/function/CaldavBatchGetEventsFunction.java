package io.github.oldmanpushcart.moss.backend.extra.caldav.function;

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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@ChatFnName("caldav_batch_get_events")
@ChatFnDescription("批量获取日历事件：根据日历事件地址列表批量查询详细信息")
public class CaldavBatchGetEventsFunction implements ChatFunction<CaldavBatchGetEventsFunction.Parameter, CaldavBatchGetEventsFunction.Result> {

    @Autowired
    private CaldavConfig config;

    @Autowired
    @Qualifier("caldavHttp")
    private OkHttpClient caldavHttp;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        CompletionStage<List<String>> stage = CompletableFuture.completedStage(new ArrayList<>());
        for (URI uri : parameter.uris()) {
            stage = stage.thenCompose(outputs ->
                    fetchEvent(uri)
                            .thenApply(output -> {
                                outputs.add(output);
                                return outputs;
                            }));
        }
        return stage
                .thenApply(Result::new);
    }

    private CompletionStage<String> fetchEvent(URI uri) {
        final var reportBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <c:filter>
                    <c:comp-filter name="VCALENDAR">
                      <c:comp-filter name="VEVENT"/>
                    </c:comp-filter>
                  </c:filter>
                  <d:prop>
                    <d:getetag/>
                    <c:calendar-data/>
                  </d:prop>
                </c:calendar-query>
                """;

        final var requestBody = RequestBody.create(reportBody, MediaType.get("text/xml;charset=utf-8"));

        final var request = new Request.Builder()
                .url(config.getHost().resolve(uri).toString())
                .header("Content-Type", "application/xml; charset=utf-8")
                .header("Depth", "0")
                .method("REPORT", requestBody)
                .build();

        return OkHttpUtils.async(caldavHttp, request)
                .thenCompose(OkHttpUtils::thenComposeStringBody);
    }


    public record Parameter(

            @JsonPropertyDescription("日历事件地址列表")
            @JsonProperty(required = true)
            List<URI> uris

    ) {

    }

    @Value
    public static class Result {

        @JsonProperty
        List<String> outputs;

        @JsonProperty
        String prompt = "返回的内容是CALDAV格式的日历详细信息，后续处理中你需要带上会议的UUID";

    }

}
