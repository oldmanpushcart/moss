package io.github.oldmanpushcart.moss.backend.extra.system.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@ChatFnName("system_datetime")
@ChatFnDescription("获取系统当前时间")
public class SystemDateTimeFunction implements ChatFunction<SystemDateTimeFunction.Parameter, SystemDateTimeFunction.Result> {

    private static final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        return CompletableFuture.completedStage(new Result(
                formatter.format(LocalDateTime.now()),
                pattern
        ));
    }

    public record Parameter() {
    }

    public record Result(

            @JsonPropertyDescription("当前时间")
            @JsonProperty
            String datetime,

            @JsonPropertyDescription("时间格式")
            @JsonProperty
            String pattern

    ) {

    }

}
