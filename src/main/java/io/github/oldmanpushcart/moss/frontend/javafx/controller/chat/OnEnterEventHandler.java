package io.github.oldmanpushcart.moss.frontend.javafx.controller.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.backend.audio.Speaker;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.github.oldmanpushcart.moss.frontend.javafx.view.AttachmentListView;
import io.github.oldmanpushcart.moss.frontend.javafx.view.MessageView;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.oldmanpushcart.moss.util.ExceptionUtils.resolveRootCause;
import static io.github.oldmanpushcart.moss.util.ExceptionUtils.stackTraceToString;
import static javafx.application.Platform.isFxApplicationThread;

@AllArgsConstructor
class OnEnterEventHandler implements EventHandler<ActionEvent> {

    private final VBox messagesBox;
    private final TextArea inputTextArea;
    private final AttachmentListView attachmentListView;
    private final ToggleButton deepThinkingToggleButton;
    private final ToggleButton enterToggleButton;
    private final AtomicBoolean autoScrollToBottomRef;

    private final AtomicBoolean autoSpeakRef;
    private final CompositeDisposableControl speakerControl;
    private final CompositeDisposableControl chatterControl;
    private final Speaker speaker;
    private final Chatter chatter;

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

            final var chattingContext = new Chatter.Context() {{
                setAttachments(selectedAttachments());
                setDeepThinking(deepThinkingToggleButton.isSelected());
            }};

            final var renderingContext = new ChatRenderingContext() {{
                final var inputText = popInputText();
                setSource(event.getSource());
                setInputText(inputText);
                setRequestMessageView(new MessageView() {{
                    setContent(inputText);
                    setButtonBarEnabled(false);
                }});
                setResponseMessageView(new MessageView() {{
                    setButtonBarEnabled(false);
                }});
            }};

            // 构建应答消息并添加到消息列表
            messagesBox.getChildren()
                    .addAll(renderingContext.getRequestMessageView(), renderingContext.getResponseMessageView());

            // 执行对话
            onChat(chatterControl.interruptAndNew(), renderingContext, chattingContext);

        } else {
            chatterControl.interrupt();
        }
    }

    // 获取输入框的文本并清空输入框
    private String popInputText() {
        assert isFxApplicationThread();
        final var inputText = inputTextArea.getText();
        inputTextArea.clear();
        return inputText;
    }

    // 获取选择的附件
    private List<File> selectedAttachments() {
        return attachmentListView.isVisible()
                ? List.copyOf(attachmentListView.selected())
                : Collections.emptyList();
    }

    // 执行对话
    private void onChat(CompositeDisposable dispose, ChatRenderingContext renderingContext, Chatter.Context chattingContext) {

        final var responseMessageView = renderingContext.getResponseMessageView();

        Platform.runLater(() -> {
            responseMessageView.setContent("思考中...");
            responseMessageView.setButtonBarEnabled(false);
        });

        final var isFirstResponseRef = new AtomicBoolean(true);

        chatter.chat(chattingContext, renderingContext.getInputText())

                /*
                 * 这里因为要进行音频播放，所以必须要对流结果进行缓存。
                 * 否则在播放的时候会重新触发流执行
                 */
                .thenApply(responseFlow -> {
                    final var replayFlow = responseFlow.replay();
                    dispose.add(replayFlow.connect());
                    return replayFlow;
                })

                // 订阅流
                .thenAccept(responseFlow -> responseFlow
                        .doOnSubscribe(sub -> renderingResponseMessageViewOnSubscribe(renderingContext, chattingContext, responseFlow))
                        .doOnNext(r -> triggerAutoSpeakInFirstResponse(isFirstResponseRef, renderingContext, r))
                        .doOnCancel(() -> renderingResponseMessageViewOnFinish(renderingContext))
                        .subscribe(
                                r -> renderingResponseMessageViewOnNext(renderingContext, r),
                                ex -> renderingResponseMessageViewOnError(renderingContext, ex),
                                () -> renderingResponseMessageViewOnFinish(renderingContext),
                                dispose
                        ))

                // 当出现异常时需要将渲染异常信息
                .whenComplete((v, ex) -> {
                    if (null != ex) {
                        renderingResponseMessageViewOnError(renderingContext, ex);
                    }
                });

    }

    // 首应答包触发自动朗读（如有）
    private void triggerAutoSpeakInFirstResponse(AtomicBoolean isFirstResponseRef, ChatRenderingContext renderingContext, ChatResponse response) {
        final var message = response.output().best().message();

        /*
         * 首应答判断
         * 这里需要过滤掉 非AI消息、空文本消息。
         * 因为会有function_call的调用返回，会被误判为可播放的语音首包
         */
        if (!isFirstResponseRef.get()
            || !Objects.equals(message.role(), Message.Role.AI)
            || !StringUtils.isNotBlank(message.text())) {
            return;
        }
        isFirstResponseRef.set(false);
        if (autoSpeakRef.get()) {
            final var responseMessageView = renderingContext.getResponseMessageView();
            responseMessageView.getSpeakToggleButton().setSelected(true);
            responseMessageView.getSpeakToggleButton().fireEvent(new ActionEvent());
        }
    }

    // 渲染响应消息
    private void renderingResponseMessageViewOnSubscribe(ChatRenderingContext renderingContext, Chatter.Context chattingContext, Flowable<ChatResponse> responseFlow) {
        final var responseMessageView = renderingContext.getResponseMessageView();
        responseMessageView.getRedoButton()
                .setOnAction(event -> {
                    event.consume();
                    autoScrollToBottomRef.set(false);
                    renderingContext
                            .setSource(event.getSource())
                            .cleanDisplayBuf();
                    chattingContext
                            .setAttachments(selectedAttachments())
                            .setDeepThinking(deepThinkingToggleButton.isSelected());
                    onChat(new CompositeDisposable(), renderingContext, chattingContext);
                });
        responseMessageView.getSpeakToggleButton()
                .setOnAction(new OnSpeakEventHandler(
                        speakerControl,
                        speaker,
                        responseFlow.map(r -> r.output().best().message().text())
                ));
    }

    // 渲染对话的显示内容
    private static StringBuilder renderingResponseDisplayBuf(ChatRenderingContext renderingContext) {
        final var displayBuf = new StringBuilder();
        final var reasoningContentDisplayBuf = renderingContext.getReasoningContentDisplayBuf();
        if (StringUtils.isNoneBlank(reasoningContentDisplayBuf)) {
            try (final var scanner = new Scanner(reasoningContentDisplayBuf.toString())) {
                while (scanner.hasNextLine()) {
                    displayBuf.append("> %s\n".formatted(scanner.nextLine()));
                }
            }
        }
        final var contentDisplayBuf = renderingContext.getContentDisplayBuf();
        if (StringUtils.isNoneBlank(contentDisplayBuf)) {
            displayBuf
                    .append(displayBuf.isEmpty() ? "" : "\n")
                    .append(contentDisplayBuf);
        }
        return displayBuf;
    }


    private void renderingResponseMessageViewOnNext(ChatRenderingContext renderingContext, ChatResponse response) {

        // 更新检索引用
        final var referenceBuf = renderingContext.getReferenceDisplayBuf();
        if (referenceBuf.isEmpty() && response.output().hasSearchInfo()) {
            response.output().searchInfo().results()
                    .forEach(result -> referenceBuf.append("> - [%s](%s)\n".formatted(
                            result.title(),
                            result.site()
                    )));
        }

        final var responseMessage = response.output().best().message();

        // 更新内容缓存
        final var contentDisplayBuf = renderingContext.getContentDisplayBuf();
        final var content = responseMessage.text();
        if (StringUtils.isNoneBlank(content)) {
            contentDisplayBuf.append(content);
        }

        // 更新思考缓存
        final var reasoningContentDisplayBuf = renderingContext.getReasoningContentDisplayBuf();
        final var reasoningContent = responseMessage.reasoningContent();
        if (StringUtils.isNoneBlank(reasoningContent)) {
            reasoningContentDisplayBuf.append(reasoningContent);
        }

        /*
         * 渲染显示内容
         */
        final var responseMessageView = renderingContext.getResponseMessageView();
        final var responseDisplayBuf = renderingResponseDisplayBuf(renderingContext);
        Platform.runLater(() -> responseMessageView.setContent(responseDisplayBuf));

    }

    private void renderingResponseMessageViewOnFinish(ChatRenderingContext renderingContext) {
        final var referenceBuf = renderingContext.getReferenceDisplayBuf();
        final var responseMessageView = renderingContext.getResponseMessageView();
        final var responseDisplayBuf = renderingResponseDisplayBuf(renderingContext)
                .append("\n")
                .append(referenceBuf);
        Platform.runLater(() -> {
            responseMessageView.setContent(responseDisplayBuf);
            responseMessageView.setButtonBarEnabled(true);
            if (renderingContext.getSource() == enterToggleButton) {
                enterToggleButton.setSelected(false);
            }
        });
    }

    private void renderingResponseMessageViewOnError(ChatRenderingContext renderingContext, Throwable ex) {
        final var responseMessageView = renderingContext.getResponseMessageView();
        final var contentDisplayBuf = renderingContext.getContentDisplayBuf();
        final var rootEx = resolveRootCause(ex);
        final var error = """
                %s
                > ERROR %s
                ```java
                %s
                ```
                """.formatted(
                contentDisplayBuf,
                rootEx.getMessage(),
                stackTraceToString(rootEx)
        );
        final var responseDisplayBuf = renderingResponseDisplayBuf(renderingContext)
                .append("\n")
                .append(error);
        Platform.runLater(() -> {
            responseMessageView.setContent(responseDisplayBuf);
            responseMessageView.setButtonBarEnabled(true);
            if (renderingContext.getSource() == enterToggleButton) {
                enterToggleButton.setSelected(false);
            }
        });
    }

}
