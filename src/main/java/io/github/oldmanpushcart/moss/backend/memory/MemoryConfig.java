package io.github.oldmanpushcart.moss.backend.memory;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 记忆体配置
 */
@Data
@ConfigurationProperties(prefix = "moss.backend.memory")
@Component
public class MemoryConfig {

    /**
     * 最大数量
     */
    private Integer maxCount = 1000;

    /**
     * 最大Tokens
     */
    private Long maxTokens = 32767L;

    /**
     * 持续时间
     */
    private Duration duration = Duration.ofDays(7);

}
