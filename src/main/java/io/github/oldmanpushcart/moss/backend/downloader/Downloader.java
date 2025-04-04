package io.github.oldmanpushcart.moss.backend.downloader;

import okhttp3.OkHttpClient;

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
     * @param http   OkHttp客户端
     * @param source 源
     * @return 下载结果
     */
    CompletionStage<URI> download(OkHttpClient http, URI source);

    /**
     * 下载
     *
     * @param http     OkHttp客户端
     * @param source   源
     * @param filename 文件名
     * @return 下载结果
     */
    CompletionStage<URI> download(OkHttpClient http, URI source, String filename);

    /**
     * 批量下载
     *
     * @param http    OkHttp客户端
     * @param sources 源
     * @return 下载结果
     */
    CompletionStage<List<URI>> downloads(OkHttpClient http, List<URI> sources);

}
