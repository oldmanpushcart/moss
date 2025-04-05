package io.github.oldmanpushcart.moss.frontend.audio;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

@SpringBootConfiguration
public class AudioConfiguration {

    @Bean(destroyMethod = "close")
    public SourceDataLineChannel audioSourceDataLineChannel(AudioConfig config) throws LineUnavailableException {
        final var sourceConfig = config.getSource();
        final var format = new AudioFormat(
                sourceConfig.getSampleRate(),
                sourceConfig.getSampleSizeInBits(),
                sourceConfig.getChannels(),
                sourceConfig.isSigned(),
                sourceConfig.isBigEndian()
        );
        final var source = AudioSystem.getSourceDataLine(format);
        source.open(format);
        return new SourceDataLineChannel(source);
    }

}
