package io.github.oldmanpushcart.moss.infra.uploader;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "moss.infra.uploader")
@Component
public class UploaderConfig {

    private Duration ossExpiresDuration = Duration.ofHours(48);
    private String cleanCronExpress = "0 */5 * * * ?";
    private Integer cleanBatchSize = 100;
    private Integer cleanQueueCapacity = 1000;

}
