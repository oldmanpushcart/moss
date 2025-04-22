package io.github.oldmanpushcart.moss.backend.chatter;

import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ChatterConfiguration {

    @Bean
    public Set<ChatFunctionTool> chatFunctionTools(Set<ChatFunction<?, ?>> functions) {
        return functions.stream()
                .map(ChatFunctionTool::of)
                .collect(Collectors.toUnmodifiableSet());
    }

}
