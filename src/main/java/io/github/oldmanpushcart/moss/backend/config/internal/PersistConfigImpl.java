package io.github.oldmanpushcart.moss.backend.config.internal;

import io.github.oldmanpushcart.moss.backend.config.PersistConfig;
import io.github.oldmanpushcart.moss.backend.config.internal.dao.PersistConfigEntryDao;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class PersistConfigImpl implements PersistConfig {

    private PersistConfigEntryDao store;

    @Override
    public String getValue(String key) {
        final var entryDO = store.getByKey(key);
        return null == entryDO ? null : entryDO.getValue();
    }

    @Override
    public boolean update(String key, String value) {
        return 1 == store.updateValue(key, value);
    }

    @Override
    public boolean update(String key, String expectedValue, String newValue) {
        return 1 == store.casUpdateValue(key, expectedValue, newValue);
    }

}
