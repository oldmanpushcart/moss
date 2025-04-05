package io.github.oldmanpushcart.moss.frontend.javafx.controller.chat;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisOptions;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.moss.frontend.audio.SourceDataLineChannel;
import io.github.oldmanpushcart.moss.frontend.util.ByteBufferObserverExchangeListener;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ToggleButton;
import lombok.AllArgsConstructor;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor
public class OnSpeakEventHandler implements EventHandler<ActionEvent> {

    private final CompositeDisposableControl speakerControl;
    private final DashscopeClient dashscope;
    private final SourceDataLineChannel sourceChannel;
    private final Flowable<String> stringFlow;

    @Override
    public void handle(ActionEvent event) {

        event.consume();
        final var button = (ToggleButton) event.getSource();

        // 朗读按钮被选中
        if (button.isSelected()) {
            final var dispose = speakerControl.interruptAndNew();
            CompletableFuture.completedStage(stringFlow)
                    .thenCompose(f -> synthesis(dispose, f))
                    .thenCompose(f -> playback(dispose, f))
                    .whenComplete((v, ex) -> Platform.runLater(() -> button.setSelected(false)));
        }

        // 朗读按钮被取消
        else {
            speakerControl.interrupt();
        }

    }

    private CompletionStage<Flowable<ByteBuffer>> synthesis(CompositeDisposable dispose, Flowable<String> stringFlow) {

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

    private CompletionStage<?> playback(CompositeDisposable dispose, Flowable<ByteBuffer> flow) {

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
