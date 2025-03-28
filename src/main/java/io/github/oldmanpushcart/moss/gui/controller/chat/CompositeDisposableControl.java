package io.github.oldmanpushcart.moss.gui.controller.chat;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

import java.util.concurrent.CountDownLatch;

/**
 *  CompositeDisposable 控制器
 */
class CompositeDisposableControl {

    // 当前正在进行的订阅
    private volatile CompositeDisposable current;

    /**
     * 停止当前订阅并开启一个新订阅
     *
     * @return 朗读资源
     */
    public synchronized CompositeDisposable interruptAndNew() {
        interrupt();
        return current = new CompositeDisposable();
    }

    /**
     * 停止当前订阅
     */
    public synchronized void interrupt() {
        if (null != current) {
            ensureDisposedSafely(current);
            current = null;
        }
    }

    // 确保释放
    private static void ensureDisposedSafely(final CompositeDisposable dispose) {

        // 如果已经释放完成，则无需处理
        if (dispose.isDisposed()) {
            return;
        }

        // 释放
        dispose.dispose();

        /*
         * 可能不会立即被释放，所以需要等待释放完成
         */
        if (!dispose.isDisposed()) {
            final var latch = new CountDownLatch(1);
            dispose.add(Disposable.fromRunnable(latch::countDown));
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

}
