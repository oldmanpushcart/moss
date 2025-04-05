package io.github.oldmanpushcart.moss.frontend.audio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "moss.backend.audio")
@Component
public class AudioConfig {

    Source source = new Source();

    @Data
    public static class Source {
        int sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
    }

}
