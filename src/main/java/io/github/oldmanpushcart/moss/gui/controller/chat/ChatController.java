package io.github.oldmanpushcart.moss.gui.controller.chat;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.moss.gui.view.AttachmentListView;
import io.github.oldmanpushcart.moss.gui.view.MessageView;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ChatController {

    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private VBox messagesBox;

    @FXML
    private AttachmentListView attachmentListView;

    @FXML
    private ToggleButton autoSpeakToggleButton;

    @FXML
    private ToggleButton attachmentToggleButton;

    @FXML
    private TextArea inputTextArea;

    @FXML
    private ToggleButton enterToggleButton;

    private final AtomicBoolean autoScrollToBottomRef = new AtomicBoolean(true);
    private final DashscopeClient dashscope;
    private final Memory memory;

    @Autowired
    public ChatController(DashscopeClient dashscope, Memory memory) {
        this.dashscope = dashscope;
        this.memory = memory;
    }

    @FXML
    private void initialize() {

        // 绑定显示附件列表
        attachmentListView.visibleProperty()
                .bind(attachmentToggleButton.selectedProperty());
        attachmentListView.managedProperty()
                .bind(attachmentToggleButton.selectedProperty());

        // 发送状态切换
        enterToggleButton.selectedProperty()
                .addListener((obs, oldValue, newValue) ->
                        enterToggleButton.setText(newValue ? "中止" : "发送"));

        // 绑定CTRL+ENTER快捷键
        inputTextArea.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode().equals(KeyCode.ENTER)) {
                event.consume();
                enterToggleButton.fire();
            }
        });

        // 输入框获取默认焦点
        Platform.runLater(() -> inputTextArea.requestFocus());

        // 绑定发送按钮事件
        enterToggleButton
                .setOnAction(new OnEnterEventHandler(
                        messagesBox,
                        inputTextArea,
                        attachmentListView,
                        enterToggleButton,
                        autoScrollToBottomRef,
                        dashscope,
                        memory
                ));

        // 绑定自动滚动
        bindingMessagesScrollPaneAutoScrollToBottom();

        // 加载历史消息
        loadingMessages();

    }

    // 从记忆体中加载历史消息
    private void loadingMessages() {
        memory.recall().forEach(fragment -> {

            final var inputText = fragment.requestMessage().text();

            messagesBox.getChildren()
                    .add(new MessageView() {{
                        setContent(inputText);
                        setButtonBarEnabled(false);
                    }});

            messagesBox.getChildren()
                    .add(new MessageView() {{
                        setContent(fragment.responseMessage().text());
                        setButtonBarEnabled(false);
                    }});

        });
    }

    // 绑定滚动条自动滚动
    private void bindingMessagesScrollPaneAutoScrollToBottom() {
        messagesBox.heightProperty()
                .addListener(new ChangeListener<Number>() {

                    @Override
                    public void changed(ObservableValue<? extends Number> obs, Number oldValue, Number newValue) {
                        if (autoScrollToBottomRef.get()) {
                            smoothScrollToBottom();
                        }
                    }

                    /**
                     * 如果内容高度超过视口高度，则滚动到底部。
                     */
                    public void smoothScrollToBottom() {

                        // 动画开始位置（当前位置）
                        final var begin = messagesScrollPane.vvalueProperty();

                        // 动画结束位置（最底部）
                        final var end = messagesScrollPane.getVmax() + messagesBox.getSpacing();

                        // 动画持续时间（毫秒）
                        final var duration = Duration.millis(500);

                        // 动画关键帧
                        final var keyFrame = new KeyFrame(duration, new KeyValue(begin, end, Interpolator.EASE_BOTH));

                        // 动画播放(只播放1次)
                        final var timeline = new Timeline(keyFrame);
                        timeline.setCycleCount(1);
                        timeline.play();
                    }

                });
    }

}
