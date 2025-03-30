package io.github.oldmanpushcart.moss.gui.controller.chat;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.gui.view.AttachmentListView;
import io.github.oldmanpushcart.moss.gui.view.MessageView;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import io.github.oldmanpushcart.moss.infra.memory.MemoryFragment;
import io.github.oldmanpushcart.moss.manager.ChatManager;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.oldmanpushcart.moss.util.ExceptionUtils.resolveRootCause;
import static io.github.oldmanpushcart.moss.util.ExceptionUtils.stackTraceToString;
import static javafx.application.Platform.isFxApplicationThread;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@AllArgsConstructor
class OnEnterEventHandler implements EventHandler<ActionEvent> {

    private final VBox messagesBox;
    private final TextArea inputTextArea;
    private final AttachmentListView attachmentListView;
    private final ToggleButton enterToggleButton;
    private final AtomicBoolean autoScrollToBottomRef;

    private final ChatManager chatManager;
    private final DashscopeClient dashscope;
    private final Memory memory;
    private final CompositeDisposableControl chatControl = new CompositeDisposableControl();

    @Override
    public void handle(ActionEvent event) {
        event.consume();
        assert enterToggleButton == event.getSource();
        if (enterToggleButton.isSelected()) {

            /*
             * 从Enter进来的请求说明用户的焦点在底部
             * 消息列表需要自动滚动到底部，方便用户使用
             */
            autoScrollToBottomRef.set(true);

            // 清空输入框并获取文本
            final var inputText = popInputText();

            // 构建新的记忆片段
            final var fragment = new MemoryFragment()
                    .requestMessage(Message.ofUser(inputText));

            // 构造请求消息并添加到消息列表
            messagesBox.getChildren()
                    .add(new MessageView() {{
                        setContent(inputText);
                        setButtonBarEnabled(false);
                    }});

            // 构建应答消息并添加到消息列表
            final var responseMessageView = new MessageView() {{
                setButtonBarEnabled(false);
                getRedoButton().setOnAction(event -> {
                    event.consume();
                    autoScrollToBottomRef.set(false);
                    onChat(event.getSource(), this, fragment, new CompositeDisposable());
                });
            }};
            messagesBox.getChildren()
                    .add(responseMessageView);

            // 执行对话
            final var dispose = chatControl.interruptAndNew();
            onChat(event.getSource(), responseMessageView, fragment, dispose);

        } else {
            chatControl.interrupt();
        }
    }

    private String popInputText() {
        assert isFxApplicationThread();
        final var inputText = inputTextArea.getText();
        inputTextArea.clear();
        return inputText;
    }

    private void onChat(Object source, MessageView responseMessageView, MemoryFragment fragment, CompositeDisposable dispose) {

        Platform.runLater(() -> {
            responseMessageView.setContent("思考中...");
            responseMessageView.setButtonBarEnabled(false);
        });

        final var attachments = attachmentListView.isVisible()
                ? attachmentListView.selected()
                : Collections.<File>emptyList();

        final var stringBuf = new StringBuilder();
        final var referenceDisplayBuf = new StringBuilder();
        chatManager.chat(fragment, attachments)
                .thenAccept(responseFlow -> responseFlow

                        // 构建引用
                        .doOnNext(response -> {
                            if (referenceDisplayBuf.isEmpty() && !response.output().search().results().isEmpty()) {
                                referenceDisplayBuf.append("> ##### 参考资料\n");
                                response.output().search().results()
                                        .forEach(result -> {
                                            referenceDisplayBuf.append("> - [%s](%s)\n".formatted(result.title(), result.site()));
                                        });
                            }
                        })

                        // 转换文本流
                        .map(response -> response.output().best().message().text())

                        // 开始订阅
                        .subscribe(

                                // 流过程
                                text -> Platform.runLater(() -> {
                                    if (isNotEmpty(text)) {
                                        responseMessageView.setContent(stringBuf.append(text));
                                    }
                                }),

                                // 流错误
                                ex -> {
                                    final var rootEx = resolveRootCause(ex);
                                    final var content = """
                                            %s
                                            > ERROR %s
                                            ```java
                                            %s
                                            ```
                                            """.formatted(
                                            stringBuf,
                                            rootEx.getMessage(),
                                            stackTraceToString(rootEx)
                                    );
                                    Platform.runLater(() -> {
                                        responseMessageView.setContent(content);
                                        responseMessageView.setButtonBarEnabled(true);
                                    });
                                },

                                // 流结束
                                () -> {

                                    final var responseDisplayBuf = new StringBuilder()
                                            .append(stringBuf)
                                            .append("\n\n")
                                            .append(referenceDisplayBuf);

                                    fragment.responseMessage(Message.ofAi(stringBuf.toString()));
                                    memory.saveOrUpdate(fragment);
                                    Platform.runLater(() -> {
                                        responseMessageView.setContent(responseDisplayBuf);
                                        responseMessageView.setButtonBarEnabled(true);
                                        if (source == enterToggleButton) {
                                            enterToggleButton.setSelected(false);
                                        }
                                    });
                                },

                                dispose
                        ));
    }

}
