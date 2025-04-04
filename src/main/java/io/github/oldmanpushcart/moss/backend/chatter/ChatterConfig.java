package io.github.oldmanpushcart.moss.backend.chatter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@ConfigurationProperties(prefix = "moss.backend.chatter")
@Component
public class ChatterConfig {

    /**
     * 系统提示词位置
     */
    Path systemPromptLocation = Paths.get("cfg", "system-prompt.md");

}
