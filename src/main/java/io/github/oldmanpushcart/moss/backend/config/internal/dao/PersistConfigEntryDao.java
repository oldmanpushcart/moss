package io.github.oldmanpushcart.moss.backend.config.internal.dao;

import io.github.oldmanpushcart.moss.backend.config.internal.domain.PersistConfigEntryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PersistConfigEntryDao {

    void insert(PersistConfigEntryDO entry);

    int updateValue(
            @Param("key") String key,
            @Param("value") String value
    );

    int casUpdateValue(
            @Param("key") String key,
            @Param("expectedValue") String expectedValue,
            @Param("newValue") String newValue
    );
    PersistConfigEntryDO getByKey(String key);

}
