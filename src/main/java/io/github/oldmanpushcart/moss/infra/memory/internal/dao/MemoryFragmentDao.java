package io.github.oldmanpushcart.moss.infra.memory.internal.dao;

import io.github.oldmanpushcart.moss.infra.memory.internal.domain.MemoryFragmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 记忆片段数据访问对象
 */
@Mapper
public interface MemoryFragmentDao {

    void insert(MemoryFragmentDO fragment);

    MemoryFragmentDO getById(long fragmentId);

    List<MemoryFragmentDO> pagingForIterator(
            @Param("maxFragmentId") long maxFragmentId,
            @Param("limit") int limit
    );

}
