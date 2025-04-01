package io.github.oldmanpushcart.moss.gui.controller.chat;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.gui.view.AttachmentListView;
import io.github.oldmanpushcart.moss.gui.view.MessageView;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import io.github.oldmanpushcart.moss.infra.memory.MemoryFragment;
import io.github.oldmanpushcart.moss.manager.MossChatContext;
import io.github.oldmanpushcart.moss.manager.MossChatManager;
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

@AllArgsConstructor
class OnEnterEventHandler implements EventHandler<ActionEvent> {

    private final VBox messagesBox;
    private final TextArea inputTextArea;
    private final AttachmentListView attachmentListView;
    private final ToggleButton enterToggleButton;
    private final AtomicBoolean autoScrollToBottomRef;

    private final MossChatManager mossChatManager;
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
        final var referenceBuf = new StringBuilder();

        final var context = MossChatContext.newBuilder()
                .fragment(fragment)
                .attachments(attachments)
                .build();

        mossChatManager.chat(context)
                .thenAccept(responseFlow -> responseFlow

                        // 构建引用
                        .doOnNext(response -> {
                            if (referenceBuf.isEmpty() && !response.output().search().results().isEmpty()) {
                                referenceBuf.append("> ##### 参考资料\n");
                                response.output().search().results()
                                        .forEach(result -> referenceBuf.append("> - [%s](%s)\n".formatted(
                                                result.title(),
                                                result.site()
                                        )));
                            }
                        })

                        // 转换文本流
                        .map(response -> response.output().best().message().text())

                        // 开始订阅
                        .subscribe(
                                text -> renderingResponseMessageViewOnNext(responseMessageView, stringBuf, text),
                                ex -> renderingResponseMessageViewOnError(responseMessageView, source, stringBuf, ex),
                                () -> {

                                    // 保存记忆片段
                                    fragment.responseMessage(Message.ofAi(stringBuf.toString()));
                                    memory.saveOrUpdate(fragment);

                                    // 渲染应答消息视图
                                    renderingResponseMessageViewOnFinish(responseMessageView, source, stringBuf, referenceBuf);

                                },
                                dispose
                        ))
                .whenComplete((v, ex) -> {
                    if (null != ex) {
                        renderingResponseMessageViewOnError(responseMessageView, source, stringBuf, ex);
                    }
                });
    }

    private void renderingResponseMessageViewOnNext(MessageView responseMessageView, StringBuilder stringBuf, String text) {
        stringBuf.append(text);
        Platform.runLater(() -> {
            if (null != text && !text.isEmpty()) {
                responseMessageView.setContent(stringBuf);
            }
        });
    }

    private void renderingResponseMessageViewOnFinish(MessageView responseMessageView, Object source, StringBuilder stringBuf, StringBuilder referenceBuf) {
        final var displayBuf = new StringBuilder()
                .append(stringBuf)
                .append("\n\n")
                .append(referenceBuf);
        Platform.runLater(() -> {
            responseMessageView.setContent(displayBuf);
            responseMessageView.setButtonBarEnabled(true);
            if (source == enterToggleButton) {
                enterToggleButton.setSelected(false);
            }
        });
    }

    private void renderingResponseMessageViewOnError(MessageView responseMessageView, Object source, StringBuilder stringBuf, Throwable ex) {
        final var rootEx = resolveRootCause(ex);
        final var error = """
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
        final var displayBuf = new StringBuilder()
                .append(stringBuf)
                .append("\n")
                .append(error);
        Platform.runLater(() -> {
            responseMessageView.setContent(displayBuf);
            responseMessageView.setButtonBarEnabled(true);
            if (source == enterToggleButton) {
                enterToggleButton.setSelected(false);
            }
        });
    }

}
