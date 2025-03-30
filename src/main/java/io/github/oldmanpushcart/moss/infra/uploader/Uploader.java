package io.github.oldmanpushcart.moss.infra.uploader;

import io.github.oldmanpushcart.dashscope4j.Model;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 上传器
 */
public interface Uploader {

    /**
     * 获取被缓存的条目，否则创建并返回
     *
     * @param model  模型
     * @param source 资源URI
     * @return 缓存条目
     */
    CompletionStage<UploadEntry> upload(Model model, URI source);

    /**
     * 列出所有已上传缓存条目
     *
     * @return 已上传缓存条目集合
     */
    List<UploadEntry> listUploaded();

    /**
     * 删除缓存条目
     *
     * @param entryId 条目ID
     * @return 删除是否成功
     */
    boolean delete(long entryId);

    /**
     * 删除缓存条目
     *
     * @param model  模型
     * @param source 资源URI
     * @return 删除是否成功
     */
    boolean delete(Model model, URI source);

}
