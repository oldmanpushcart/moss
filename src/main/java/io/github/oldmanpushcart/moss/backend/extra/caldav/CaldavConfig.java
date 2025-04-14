package io.github.oldmanpushcart.moss.backend.extra.caldav;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "moss.backend.extra.caldav")
@Component
public class CaldavConfig {

    private boolean enabled = false;
    private URI host;
    private String username;
    private String password;
    private String location;
    private HttpConfig http = new HttpConfig();

    public URI home() {
        return host.resolve(location);
    }

    @Data
    public static class HttpConfig {
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration writeTimeout = Duration.ofSeconds(30);
    }

}
