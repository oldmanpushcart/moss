package io.github.oldmanpushcart.moss.backend.config;

/**
 * 持久化配置
 */
public interface PersistConfig {

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    String getValue(String key);

    /**
     * 更新配置
     *
     * @param key   配置键
     * @param value 配置值
     * @return 是否更新成功
     */
    boolean update(String key, String value);

    /**
     * 更新配置
     *
     * @param key           配置键
     * @param expectedValue 预期值
     * @param newValue      配置值
     * @return 是否更新成功
     */
    boolean update(String key, String expectedValue, String newValue);

}
