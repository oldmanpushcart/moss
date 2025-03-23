package io.github.oldmanpushcart.moss.infra.memory.internal;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.LocalTokenizerUtils;
import io.github.oldmanpushcart.dashscope4j.util.MessageCodec;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import io.github.oldmanpushcart.moss.infra.memory.MemoryConfig;
import io.github.oldmanpushcart.moss.infra.memory.internal.dao.MemoryFragmentDao;
import io.github.oldmanpushcart.moss.infra.memory.internal.domain.MemoryFragmentDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel.Mode.MULTIMODAL;

/**
 * 记忆体实现
 */
@Component
public class MemoryImpl implements Memory {

    private final MemoryFragmentDao store;
    private final FragmentCache cache;

    @Autowired
    public MemoryImpl(MemoryConfig config, MemoryFragmentDao store) {
        this.store = store;
        this.cache = new FragmentCache(config);
        load();
    }

    /*
     * 从持久化存储中加载记忆片段
     */
    private void load() {
        pagingForLoad(Long.MAX_VALUE, 100);
    }

    /*
     * 从持久化存储中翻页加载记忆片段
     */
    private void pagingForLoad(long maxFragmentId, int limit) {

        // 如果缓存已满，则不再加载
        if (cache.isOverflow()) {
            return;
        }

        // 从持久化存储中查询一页数据
        final var fragmentDOs = store.pagingForIterator(maxFragmentId, limit);

        // 将一页记忆片段数据加载到缓存中
        if (!cache.load(() -> fragmentDOs.stream().map(Fragment::fromMemoryFragmentDO).toList())) {
            return;
        }

        // 查询的数据不满足一页，说明没有下一页
        if (fragmentDOs.size() < limit) {
            return;
        }

        // 找到当前页最小的游标，继续翻页查询
        final var minFragmentId = fragmentDOs.get(fragmentDOs.size() - 1).getFragmentId();
        pagingForLoad(minFragmentId, limit);

    }

    @Override
    public List<Message> recall() {
        return cache.elements()
                .stream()
                .flatMap(f -> Stream.of(f.requestMessage(), f.requestMessage()))
                .toList();
    }


    @Override
    public void append(String uuid, Message requestMessage, Message responseMessage) {

        // 添加到存储
        final var fragmentDO = new MemoryFragmentDO()
                .setUuid(uuid)
                .setTokens(computeMessageTokens(requestMessage) + computeMessageTokens(responseMessage))
                .setRequestMessageJson(MessageCodec.encodeToJson(MULTIMODAL, requestMessage))
                .setResponseMessageJson(MessageCodec.encodeToJson(MULTIMODAL, responseMessage));
        store.insert(fragmentDO);

        // 重新查询回来
        final var createdDO = store.getById(fragmentDO.getFragmentId());

        // 添加到缓存
        final var fragment = Fragment.fromMemoryFragmentDO(createdDO);
        cache.append(fragment);

    }

    // 计算消息所消耗的TOKENS
    private static long computeMessageTokens(Message message) {
        return LocalTokenizerUtils.encode(message.text()).size();
    }


}
