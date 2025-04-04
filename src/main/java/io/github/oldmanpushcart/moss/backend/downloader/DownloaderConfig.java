package io.github.oldmanpushcart.moss.backend.downloader;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Data
@ConfigurationProperties(prefix = "moss.backend.downloader")
@Component
public class DownloaderConfig {

    /**
     * 下载目录
     */
    private Path location = Path.of(System.getProperty("user.dir"),"download");

}
