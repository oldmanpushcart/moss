package io.github.oldmanpushcart.moss.infra.downloader.internal;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.util.HttpUtils;
import io.github.oldmanpushcart.moss.infra.downloader.Downloader;
import io.github.oldmanpushcart.moss.infra.downloader.DownloaderConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class DownloaderImpl implements Downloader {

    private final DownloaderConfig config;
    private final DashscopeClient dashscope;

    @Override
    public CompletionStage<URI> download(URI source) {
        return HttpUtils.fetchAsTempFile(dashscope.base().http(), source)
                .thenApply(temp -> {
                    try {

                        final var tempPath = temp.toPath();
                        final var targetPath = newTargetPath(config.getLocation(), source);

                        if (!Files.exists(targetPath.getParent())) {
                            Files.createDirectories(targetPath.getParent());
                        }

                        Files.copy(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        return targetPath.toUri();

                    } catch (IOException ioEx) {
                        throw new RuntimeException("Download failed!", ioEx);
                    }
                });
    }

    @Override
    public CompletionStage<List<URI>> downloads(List<URI> sources) {
        CompletionStage<List<URI>> stage = CompletableFuture.completedStage(new ArrayList<>());
        for (final var source : sources) {
            stage = stage.thenCompose(list ->
                    download(source)
                            .thenApply(uri -> {
                                list.add(uri);
                                return list;
                            }));
        }
        return stage;
    }

    private static Path newTargetPath(Path location, URI source) {
        final var now = LocalDateTime.now();
        return location
                .resolve(String.format("%02d", now.getHour()))
                .resolve(String.format("%02d", now.getMinute()))
                .resolve(Path.of(source.getPath()).getFileName());
    }


}
