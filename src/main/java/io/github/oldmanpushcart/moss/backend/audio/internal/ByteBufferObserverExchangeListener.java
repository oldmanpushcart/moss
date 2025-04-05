package io.github.oldmanpushcart.moss.backend.audio.internal;

import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 数据流监听器
 * <p>
 * 提取数据交换中的数据，并转换为数据流
 * </p>
 *
 * @param <T> 数据交换请求类型
 * @param <R> 数据交换应答类型
 */
class ByteBufferObserverExchangeListener<T, R> implements Exchange.Listener<T, R> {

    private final CompletableFuture<Observable<ByteBuffer>> completed = new CompletableFuture<>();
    private final Subject<ByteBuffer> subject = PublishSubject.create();

    @Override
    public void onOpen(Exchange<T> exchange) {
        completed.complete(subject);
    }

    @Override
    public void onByteBuffer(ByteBuffer buf) {
        subject.onNext(buf);
    }

    @Override
    public void onCompleted() {
        subject.onComplete();
    }

    @Override
    public void onError(Throwable ex) {
        subject.onError(ex);
    }

    public CompletionStage<Observable<ByteBuffer>> toObservable() {
        return completed;
    }

}