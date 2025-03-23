package io.github.oldmanpushcart.moss.infra.dashscope;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "moss.infra.dashscope")
@Component
public class DashscopeConfig {

    private String apiKey;
    private HttpConfig http = new HttpConfig();

    @Data
    public static class HttpConfig {
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration writeTimeout = Duration.ofSeconds(30);
        private Duration pingInterval = Duration.ofSeconds(10);
    }

}
