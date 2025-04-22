package io.github.oldmanpushcart.moss.frontend.javafx.controller.chat;

import io.github.oldmanpushcart.moss.backend.audio.Speaker;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.github.oldmanpushcart.moss.backend.config.PersistConfig;
import io.github.oldmanpushcart.moss.backend.memory.Memory;
import io.github.oldmanpushcart.moss.backend.uploader.Uploader;
import io.github.oldmanpushcart.moss.frontend.javafx.view.AttachmentListView;
import io.github.oldmanpushcart.moss.frontend.javafx.view.MessageView;
import io.github.oldmanpushcart.moss.frontend.javafx.view.UploaderView;
import io.reactivex.rxjava3.core.Flowable;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.oldmanpushcart.moss.backend.config.PersisConfigConstants.KEY_MEMORY_RECALL_MIN_FRAGMENT_ID;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Component
public class ChatController {

    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private VBox messagesBox;

    @FXML
    private UploaderView uploaderView;

    @FXML
    private AttachmentListView attachmentListView;

    @FXML
    private ToggleButton deepThinkingToggleButton;

    @FXML
    private ToggleButton autoSpeakToggleButton;

    @FXML
    private ToggleButton attachmentToggleButton;

    @FXML
    private ToggleButton knowledgeToggleButton;

    @FXML
    private ToggleButton webSearchToggleButton;

    @FXML
    private ToggleButton uploaderToggleButton;

    @FXML
    private TextArea inputTextArea;

    @FXML
    private ToggleButton enterToggleButton;

    @FXML
    private Pane controlPane;

    @FXML
    private MenuItem clearMemoryMenuItem;

    private final Speaker speaker;
    private final Chatter chatter;
    private final Uploader uploader;
    private final Memory memory;
    private final PersistConfig persistConfig;

    private final AtomicBoolean autoSpeakRef = new AtomicBoolean(false);
    private final AtomicBoolean autoScrollToBottomRef = new AtomicBoolean(true);
    private final CompositeDisposableControl speakerControl = new CompositeDisposableControl();
    private final CompositeDisposableControl chatterControl = new CompositeDisposableControl();

    @FXML
    private void initialize() {

        // 绑定自定朗读标记
        autoSpeakToggleButton
                .setOnAction(event -> {
                    final var isSelected = autoSpeakToggleButton.isSelected();
                    autoSpeakRef.set(isSelected);
                    if (!isSelected) {
                        speakerControl.interrupt();
                    }
                });

        // 初始化附件列表
        attachmentListView
                .bindEnabledProperty(attachmentToggleButton.selectedProperty());

        // 初始化上传列表
        uploaderView
                .bindEnabledProperty(uploaderToggleButton.selectedProperty())
                .setUploader(uploader)
                .load();

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

        // 当输入框内容为空时，禁止发送按钮启用
        enterToggleButton.disableProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> StringUtils.isBlank(inputTextArea.getText()) && !enterToggleButton.isSelected(),
                        inputTextArea.textProperty(),
                        enterToggleButton.selectedProperty()
                ));

        // 绑定发送按钮事件
        enterToggleButton
                .setOnAction(new OnEnterEventHandler(
                        messagesBox,
                        inputTextArea,
                        attachmentListView,
                        deepThinkingToggleButton,
                        knowledgeToggleButton,
                        webSearchToggleButton,
                        enterToggleButton,
                        autoScrollToBottomRef,
                        autoSpeakRef,
                        speakerControl,
                        chatterControl,
                        speaker,
                        chatter
                ));

        // 清除历史记录菜单
        clearMemoryMenuItem.setOnAction(event -> {

            /*
             * 获取最大片段ID
             * 如果为空则说明当前没有任何对话，不需要进行清空操作
             */
            final var maxFragmentId = memory.getMaxFragmentId();
            if (null == maxFragmentId) {
                return;
            }

            /*
             * 重置最小片段ID
             * 清空对话聊天内容
             */
            if (persistConfig.update(KEY_MEMORY_RECALL_MIN_FRAGMENT_ID, String.valueOf(maxFragmentId))) {
                messagesBox.getChildren().clear();
            }

        });


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

    public void loadingMemory(List<Memory.Fragment> fragments) {
        fragments.forEach(fragment -> {
            final var inputText = fragment.getRequestMessage().text();
            messagesBox.getChildren()
                    .add(new MessageView() {{
                        setContent(inputText);
                        setButtonBarEnabled(false);
                    }});
            messagesBox.getChildren()
                    .add(new MessageView() {{
                        setContent(fragment.getResponseMessage().text());
                        setButtonBarEnabled(true);
                        setRedoButtonEnabled(false);
                        getSpeakToggleButton()
                                .setOnAction(new OnSpeakEventHandler(
                                        speakerControl,
                                        speaker,
                                        Flowable.just(fragment.getResponseMessage().text())
                                ));
                    }});
        });
    }

    public void lockControlPane() {
        controlPane.setDisable(true);
    }

    public void unlockControlPane() {
        controlPane.setDisable(false);
    }

    public void focusInputText() {
        inputTextArea.requestFocus();
    }

}
