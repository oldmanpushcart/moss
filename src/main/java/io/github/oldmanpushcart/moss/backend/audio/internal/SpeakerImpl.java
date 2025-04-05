package io.github.oldmanpushcart.moss.backend.audio.internal;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisOptions;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.moss.backend.audio.AudioConfig;
import io.github.oldmanpushcart.moss.backend.audio.SourceDataLineChannel;
import io.github.oldmanpushcart.moss.backend.audio.Speaker;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Component
public class SpeakerImpl implements Speaker {

    private final AudioConfig config;
    private final DashscopeClient dashscope;
    private final SourceDataLineChannel sourceChannel;

    @Override
    public CompletionStage<Flowable<ByteBuffer>> synthesis(Flowable<String> stringFlow, CompositeDisposable dispose) {
        final var synthesisRequest = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.COSYVOICE_V1_LONGXIAOCHUN)
                .option(SpeechSynthesisOptions.SAMPLE_RATE, Constants.SAMPLE_RATE_16K)
                .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisOptions.Format.PCM)
                .build();

        final var synthesisRequestFlow = stringFlow
                .map(text -> SpeechSynthesisRequest.newBuilder(synthesisRequest)
                        .text(text)
                        .build());

        final var exchangeListener = new ByteBufferObserverExchangeListener<SpeechSynthesisRequest, SpeechSynthesisResponse>();
        dashscope.audio().synthesis()
                .exchange(synthesisRequest, Exchange.Mode.DUPLEX, exchangeListener)
                .thenAccept(exchange -> {

                    // 订阅输入文本流，在此转换为音频流
                    exchange.subscribeForWriteData(synthesisRequestFlow, true);

                    /*
                     * 如果订阅被取消，则需要主动关闭exchange
                     * 不然会导致exchange继续在后台接收数据，白白浪费计算资源和大模型费用
                     */
                    dispose.add(Disposable.fromRunnable(() -> {
                        if (!exchange.closing()) {
                            exchange.abort();
                        }
                    }));

                });

        return exchangeListener.toObservable()
                .thenApply(obs -> obs.toFlowable(BackpressureStrategy.BUFFER));
    }

    @Override
    public CompletionStage<?> playback(Flowable<ByteBuffer> flow, CompositeDisposable dispose) {
        final var completed = new CompletableFuture<>();

        final var playbackDispose = flow
                .observeOn(Schedulers.io())
                .doOnSubscribe(sub -> clearAndStart())
                .doOnTerminate(this::stopAndClear)
                .doOnCancel(this::stopAndClear)
                .subscribe(
                        sourceChannel::write,
                        completed::completeExceptionally,
                        () -> completed.complete(null)
                );

        dispose.add(playbackDispose);
        dispose.add(Disposable.fromRunnable(() -> {
            if (!completed.isDone()) {
                completed.cancel(true);
            }
        }));

        return completed;
    }

    private synchronized void clearAndStart() {
        sourceChannel.flush();
        sourceChannel.start();
    }

    private synchronized void stopAndClear() {
        sourceChannel.drain();
        sourceChannel.stop();
    }

}
