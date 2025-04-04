package io.github.oldmanpushcart.moss.frontend.javafx.controller.chat;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.github.oldmanpushcart.moss.backend.memory.Memory;
import io.github.oldmanpushcart.moss.backend.uploader.Uploader;
import io.github.oldmanpushcart.moss.frontend.javafx.view.AttachmentListView;
import io.github.oldmanpushcart.moss.frontend.javafx.view.MessageView;
import io.github.oldmanpushcart.moss.frontend.javafx.view.UploaderListView;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ChatController {

    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private VBox messagesBox;

    @FXML
    private UploaderListView uploaderListView;

    @FXML
    private AttachmentListView attachmentListView;

    @FXML
    private ToggleButton deepThinkingToggleButton;

    @FXML
    private ToggleButton autoSpeakToggleButton;

    @FXML
    private ToggleButton attachmentToggleButton;

    @FXML
    private ToggleButton uploaderToggleButton;

    @FXML
    private TextArea inputTextArea;

    @FXML
    private ToggleButton enterToggleButton;

    @FXML
    private Pane controlPane;

    private final AtomicBoolean autoScrollToBottomRef = new AtomicBoolean(true);
    private final Chatter chatter;
    private final Uploader uploader;

    @Autowired
    public ChatController(Chatter chatter, DashscopeClient dashscope, Uploader uploader) {
        this.chatter = chatter;
        this.uploader = uploader;
    }

    @FXML
    private void initialize() {

        // 绑定显示附件列表
        attachmentListView.visibleProperty()
                .bind(attachmentToggleButton.selectedProperty());
        attachmentListView.managedProperty()
                .bind(attachmentToggleButton.selectedProperty());

        initializeUploaderListView();

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
                        deepThinkingToggleButton,
                        enterToggleButton,
                        autoScrollToBottomRef,
                        chatter
                ));

        // 绑定自动滚动
        bindingMessagesScrollPaneAutoScrollToBottom();

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

    private void initializeUploaderListView() {

        // 绑定显示上传列表
        uploaderListView.visibleProperty()
                .bind(uploaderToggleButton.selectedProperty());
        uploaderListView.managedProperty()
                .bind(uploaderToggleButton.selectedProperty());
        uploaderToggleButton.selectedProperty()
                .addListener((obs, oldValue, newValue) -> {
                    if (newValue) {
                        uploaderListView.load();
                    }
                });

        // 绑定显示上传列表
        uploaderListView
                .setOnLoadAction(uploader::listUploaded)
                .setOnDeleteAction(entries ->
                        entries.forEach(entry -> uploader.delete(entry.entryId())))
                .load();
    }


    /**
     * 加载历史消息
     */
    public void loadingMemory(List<Memory.Fragment> fragments) {
        fragments.forEach(fragment -> {
            final var inputText = fragment.requestMessage().text();
            messagesBox.getChildren()
                    .add(new MessageView() {{
                        setContent(inputText);
                        setButtonBarEnabled(false);
                    }});
            messagesBox.getChildren()
                    .add(new MessageView() {{
                        setContent(fragment.responseMessage().text());
                        setButtonBarEnabled(true);
                        setRedoButtonEnabled(false);
                    }});
        });
    }

    /**
     * 锁定操作面板
     */
    public void lockControlPane() {
        controlPane.setDisable(true);
    }

    /**
     * 解锁操作面板
     */
    public void unlockControlPane() {
        controlPane.setDisable(false);
    }

}
