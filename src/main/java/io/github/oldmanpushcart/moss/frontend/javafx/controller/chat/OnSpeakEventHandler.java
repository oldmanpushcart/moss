package io.github.oldmanpushcart.moss.frontend.javafx.controller.chat;

import io.github.oldmanpushcart.moss.backend.audio.Speaker;
import io.reactivex.rxjava3.core.Flowable;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ToggleButton;
import lombok.AllArgsConstructor;

import static java.util.concurrent.CompletableFuture.completedStage;

/**
 * 朗读按钮事件处理器
 */
@AllArgsConstructor
public class OnSpeakEventHandler implements EventHandler<ActionEvent> {

    private final CompositeDisposableControl speakerControl;
    private final Speaker speaker;
    private final Flowable<String> stringFlow;

    @Override
    public void handle(ActionEvent event) {

        event.consume();
        final var button = (ToggleButton) event.getSource();

        // 朗读按钮被选中
        if (button.isSelected()) {
            final var dispose = speakerControl.interruptAndNew();
            completedStage(stringFlow)
                    .thenCompose(f -> speaker.synthesis(f, dispose))
                    .thenCompose(f -> speaker.playback(f, dispose))
                    .whenComplete((v, ex) -> Platform.runLater(() -> button.setSelected(false)));
        }

        // 朗读按钮被取消
        else {
            speakerControl.interrupt();
        }

    }


}
