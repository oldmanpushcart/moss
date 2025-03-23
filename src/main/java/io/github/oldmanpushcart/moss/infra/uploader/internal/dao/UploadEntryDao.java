package io.github.oldmanpushcart.moss.infra.uploader.internal.dao;

import io.github.oldmanpushcart.moss.infra.uploader.internal.domain.UploadEntryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 上传条目数据库访问接口
 */
@Mapper
public interface UploadEntryDao {

    void insert(UploadEntryDO entry);

    UploadEntryDO getById(long entryId);

    UploadEntryDO getByUploadKey(String uploadKey);

    List<UploadEntryDO> queryForClean(int limit);

    int update(UploadEntryDO entry);

    int deleteById(long entryId);

}
