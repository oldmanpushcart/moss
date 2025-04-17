package io.github.oldmanpushcart.moss.backend.memory.internal.dao;

import io.github.oldmanpushcart.moss.backend.memory.internal.domain.MemoryFragmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 记忆片段数据访问对象
 */
@Mapper
public interface MemoryFragmentDao {

    void insert(MemoryFragmentDO fragment);

    int update(MemoryFragmentDO fragment);

    MemoryFragmentDO getById(
            @Param("fragmentId") long fragmentId
    );

    Long getMaxFragmentId();

    List<MemoryFragmentDO> pagingForIterator(
            @Param("beginFragmentId") long beginFragmentId,
            @Param("endFragmentId") long endFragmentId,
            @Param("limit") int limit
    );

}
