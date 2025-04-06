package io.github.oldmanpushcart.moss.backend.uploader;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "moss.backend.uploader")
@Component
public class UploaderConfig {

    private Duration ossExpiresDuration = Duration.ofHours(48);

}
