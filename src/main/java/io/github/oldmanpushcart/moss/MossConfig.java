package io.github.oldmanpushcart.moss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@ConfigurationProperties(prefix = "moss")
@Component
public class MossConfig {

    Path systemPromptLocation = Paths.get("cfg", "system-prompt.md");

}
