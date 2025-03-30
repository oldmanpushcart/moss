package io.github.oldmanpushcart.moss.infra.downloader;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 下载器
 */
public interface Downloader {

    /**
     * 下载
     *
     * @param source 源
     * @return 下载结果
     */
    CompletionStage<URI> download(URI source);

    /**
     * 批量下载
     *
     * @param sources 源
     * @return 下载结果
     */
    CompletionStage<List<URI>> downloads(List<URI> sources);

}
