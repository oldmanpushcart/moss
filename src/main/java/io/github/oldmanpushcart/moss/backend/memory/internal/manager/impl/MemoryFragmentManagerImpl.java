package io.github.oldmanpushcart.moss.backend.memory.internal.manager.impl;

import io.github.oldmanpushcart.moss.backend.memory.internal.dao.MemoryFragmentDao;
import io.github.oldmanpushcart.moss.backend.memory.internal.domain.MemoryFragmentDO;
import io.github.oldmanpushcart.moss.backend.memory.internal.manager.MemoryFragmentManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.util.Collections.emptyIterator;

@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Component
public class MemoryFragmentManagerImpl implements MemoryFragmentManager {

    private final MemoryFragmentDao memoryFragmentDao;

    @Override
    public Long getMaxFragmentId() {
        return memoryFragmentDao.getMaxFragmentId();
    }

    @Override
    public void saveOrUpdate(MemoryFragmentDO fragmentDO) {
        if (null == fragmentDO.getFragmentId()) {
            memoryFragmentDao.insert(fragmentDO);
        } else {
            memoryFragmentDao.update(fragmentDO);
        }
    }

    @Override
    public MemoryFragmentDO getById(long fragmentId) {
        return memoryFragmentDao.getById(fragmentId);
    }

    @Override
    public Iterator<MemoryFragmentDO> iterator(long beginFragmentId, long endFragmentId) {
        return new Iterator<>() {

            private static final int PAGE_SIZE = 100;
            private Iterator<MemoryFragmentDO> pageIt = emptyIterator();
            private long pageMaxFragmentId = beginFragmentId;

            /**
             * 获取下一页
             *
             * @return 是否获取到下一页
             */
            private boolean fetchNextPage() {

                // 翻页查询下一页数据
                final var nextPage = memoryFragmentDao.pagingForIterator(pageMaxFragmentId, endFragmentId, PAGE_SIZE);
                if (null != nextPage && !nextPage.isEmpty()) {
                    pageMaxFragmentId = nextPage.get(nextPage.size() - 1).getFragmentId();
                    pageIt = nextPage.iterator();
                    return true;
                }

                /*
                 * 如果没有更多数据
                 * 则标记pageIt为null，中止后续所有的判断
                 */
                pageIt = null;
                return false;

            }

            @Override
            public boolean hasNext() {
                return null != pageIt && (pageIt.hasNext() || fetchNextPage());
            }

            @Override
            public MemoryFragmentDO next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return pageIt.next();
            }

        };
    }

}
