package io.github.oldmanpushcart.moss.backend.knowledge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "moss.backend.knowledge")
@Component
public class KnowledgeConfig {

    private Path location;
    private List<String> allowMimeWildcards = new ArrayList<>();

    private Chunk chunk;

    @Data
    public static class Chunk {
        private int size = 8000;
        private int overlap = 100;
    }

}
