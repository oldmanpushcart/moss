package io.github.oldmanpushcart.moss.gui.controller.chat;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.moss.gui.view.AttachmentListView;
import io.github.oldmanpushcart.moss.gui.view.MessageView;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatController {

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
                .addListener((obs, oldValue, newValue) -> {
                    if (newValue) {
                        enterToggleButton.setText("中止");
                    } else {
                        enterToggleButton.setText("发送");
                    }
                });

        // 绑定CTRL+ENTER快捷键
        inputTextArea.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode().equals(KeyCode.ENTER)) {
                enterToggleButton.fire();
                event.consume();
            }
        });

        // 绑定CTRL+ENTER快捷键
        Platform.runLater(() -> inputTextArea.requestFocus());

        // 绑定发送按钮事件
        enterToggleButton
                .setOnAction(new OnEnterEventHandler(
                        messagesBox,
                        inputTextArea,
                        attachmentListView,
                        enterToggleButton,
                        dashscope,
                        memory
                ));

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

}
