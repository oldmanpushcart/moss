package io.github.oldmanpushcart.moss.backend.uploader.internal.dao;

import io.github.oldmanpushcart.moss.backend.uploader.internal.domain.UploadEntryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 上传条目数据库访问接口
 */
@Mapper
public interface UploadEntryDao {

    /**
     * 插入上传条目
     *
     * @param entry 上传条目
     */
    void insert(UploadEntryDO entry);

    /**
     * 根据ID获取上传条目
     *
     * @param entryId 上传条目ID
     * @return 上传条目
     */
    UploadEntryDO getById(long entryId);

    /**
     * 根据上传唯一键获取上传条目
     *
     * @param uniqueKey 上传唯一键
     * @return 上传条目
     */
    UploadEntryDO getByUniqueKey(String uniqueKey);

    List<UploadEntryDO> listAll();

    /**
     * 根据ID删除上传条目
     *
     * @param entryId 上传条目ID
     * @return 删除记录数
     */
    int deleteById(long entryId);

}
