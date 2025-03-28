package io.github.oldmanpushcart.moss.gui.controller.chat;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.gui.view.AttachmentListView;
import io.github.oldmanpushcart.moss.gui.view.MessageView;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import io.github.oldmanpushcart.moss.infra.memory.MemoryFragment;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import io.reactivex.rxjava3.disposables.Disposable;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.oldmanpushcart.moss.util.ExceptionUtils.resolveRootCause;
import static io.github.oldmanpushcart.moss.util.ExceptionUtils.stackTraceToString;
import static io.github.oldmanpushcart.moss.util.FileUtils.probeContentType;
import static javafx.application.Platform.isFxApplicationThread;

@AllArgsConstructor
class OnEnterEventHandler implements EventHandler<ActionEvent> {

    private final VBox messagesBox;
    private final TextArea inputTextArea;
    private final AttachmentListView attachmentListView;
    private final ToggleButton enterToggleButton;
    private final AtomicBoolean autoScrollToBottomRef;

    private final DashscopeClient dashscope;
    private final Memory memory;
    private final CompositeDisposableControl chatControl = new CompositeDisposableControl();

    @Override
    public void handle(ActionEvent event) {
        event.consume();
        assert enterToggleButton == event.getSource();
        if (enterToggleButton.isSelected()) {

            autoScrollToBottomRef.set(true);

            // 清空输入框并获取文本
            final var inputText = popInputText();

            // 构造请求消息并添加到消息列表
            messagesBox.getChildren()
                    .add(new MessageView() {{
                        setContent(inputText);
                        setButtonBarEnabled(false);
                    }});

            // 构建应答消息并添加到消息列表
            final var responseMessageView = new MessageView() {{
                setButtonBarEnabled(false);
            }};
            messagesBox.getChildren()
                    .add(responseMessageView);

            final var fragment = new MemoryFragment()
                    .requestMessage(Message.ofUser(inputText));

            // 执行对话
            chatControl.interruptAndNew()
                    .add(onChat(event.getSource(), responseMessageView, fragment));

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

    private Disposable onChat(Object source, MessageView responseMessageView, MemoryFragment fragment) {

        Platform.runLater(()-> {
            responseMessageView.setContent("思考中...");
            responseMessageView.setButtonBarEnabled(false);
        });

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_MAX)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .building(builder ->
                        memory.recall(fragment.fragmentId())
                                .forEach(f -> {
                                    builder.addMessage(f.requestMessage());
                                    builder.addMessage(f.responseMessage());
                                }))
                .addMessage(rewriteUserMessage(fragment.requestMessage()))
                .build();

        final var stringBuf = new StringBuilder();
        return dashscope.chat().directFlow(request)

                // 转换文本流
                .map(response -> response.output().best().message().text())

                // 流开始
                .doOnSubscribe(sub -> Platform.runLater(() ->
                        responseMessageView.getRedoButton().setOnAction(event -> {
                            event.consume();
                            autoScrollToBottomRef.set(false);
                            onChat(event.getSource(), responseMessageView, fragment);
                        })))

                // 开始订阅
                .subscribe(

                        // 流过程
                        text -> Platform.runLater(() -> responseMessageView.setContent(stringBuf.append(text))),

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
                            fragment.responseMessage(Message.ofAi(stringBuf.toString()));
                            memory.saveOrUpdate(fragment);
                            Platform.runLater(() -> {
                                responseMessageView.setButtonBarEnabled(true);
                                if (source == enterToggleButton) {
                                    enterToggleButton.setSelected(false);
                                }
                            });
                        }
                );
    }

    private Message rewriteUserMessage(Message userMessage) {
        final var resource = attachmentListView.selected()
                .stream()
                .filter(file -> file.exists() && file.canRead() && file.isFile())
                .map(file ->
                        new HashMap<String, Object>() {{
                            put("mime", probeContentType(file));
                            put("uri", file.toURI());
                        }})
                .toList();
        return Message.ofUser("""
                用户输入：
                %s
                
                引用资料：
                %s
                """.formatted(
                userMessage.text(),
                JacksonUtils.toJson(resource)
        ));
    }

}
