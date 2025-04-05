package io.github.oldmanpushcart.moss.backend.audio;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

/**
 * 语音合成器
 */
public interface Speaker {

    /**
     * 合成语音
     *
     * @param stringFlow 文本流
     * @param dispose    释放资源
     * @return 语音流
     */
    CompletionStage<Flowable<ByteBuffer>> synthesis(Flowable<String> stringFlow, CompositeDisposable dispose);

    /**
     * 播放语音
     *
     * @param flow    语音流
     * @param dispose 释放资源
     * @return 播放行为
     */
    CompletionStage<?> playback(Flowable<ByteBuffer> flow, CompositeDisposable dispose);

}
