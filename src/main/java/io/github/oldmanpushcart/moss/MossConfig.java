package io.github.oldmanpushcart.moss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "moss")
@Component
public class MossConfig {

}
