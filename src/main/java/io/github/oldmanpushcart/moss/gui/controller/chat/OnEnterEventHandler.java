package io.github.oldmanpushcart.moss.gui.controller.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.gui.view.AttachmentListView;
import io.github.oldmanpushcart.moss.gui.view.MessageView;
import io.github.oldmanpushcart.moss.manager.MossChatManager;
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

    private final MossChatManager mossChatManager;
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

            final var request = ChatRequest.newBuilder()
                    .context(MossChatManager.Context.class,
                            new MossChatManager.Context() {{
                                setAttachments(selectedAttachments());
                            }})
                    .context(MossChatRenderingContext.class,
                            new MossChatRenderingContext() {{
                                setSource(event.getSource());
                                setResponseMessageView(responseMessageView);
                            }})
                    .model(decideChatModel())
                    .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                    .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                    .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption() {{
                        forcedSearch(true);
                        searchStrategy(SearchStrategy.STANDARD);
                        enableSource();
                    }})
                    .addMessage(Message.ofUser(inputText))
                    .build();

            responseMessageView.getRedoButton()
                    .setOnAction(e -> {
                        e.consume();
                        autoScrollToBottomRef.set(false);
                        final var newRequest = ChatRequest.newBuilder(request)
                                .model(decideChatModel())
                                .building(builder -> {
                                    final var context = request.context(MossChatManager.Context.class)
                                            .setAttachments(selectedAttachments());
                                    final var renderingContext = request.context(MossChatRenderingContext.class)
                                            .setSource(e)
                                            .cleanDisplayBuf();
                                    builder.context(MossChatManager.Context.class, context);
                                    builder.context(MossChatRenderingContext.class, renderingContext);
                                })
                                .build();
                        onChat(newRequest, new CompositeDisposable());
                    });

            // 执行对话
            final var dispose = chatControl.interruptAndNew();
            onChat(request, dispose);

        } else {
            chatControl.interrupt();
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

    // 决定采用那个对话模型
    private ChatModel decideChatModel() {
        return deepThinkingToggleButton.isSelected()
                ? ChatModel.QWQ_PLUS
                : ChatModel.QWEN_MAX;
    }

    // 执行对话
    private void onChat(ChatRequest request, CompositeDisposable dispose) {

        final var renderingContext = request.context(MossChatRenderingContext.class);
        final var responseMessageView = renderingContext.getResponseMessageView();

        Platform.runLater(() -> {
            responseMessageView.setContent("思考中...");
            responseMessageView.setButtonBarEnabled(false);
        });

        mossChatManager.chat(request)
                .thenAccept(responseFlow -> responseFlow
                        .subscribe(
                                r -> renderingResponseMessageViewOnNext(renderingContext, r),
                                ex -> renderingResponseMessageViewOnError(renderingContext, ex),
                                () -> renderingResponseMessageViewOnFinish(renderingContext),
                                dispose
                        ))

                .whenComplete((v, ex) -> {
                    if (null != ex) {
                        renderingResponseMessageViewOnError(renderingContext, ex);
                    }
                });
    }

    // 渲染对话的显示内容
    private static StringBuilder renderingResponseDisplayBuf(MossChatRenderingContext context) {
        final var displayBuf = new StringBuilder();
        final var reasoningContentDisplayBuf = context.getReasoningContentDisplayBuf();
        if (StringUtils.isNoneBlank(reasoningContentDisplayBuf)) {
            try (final var scanner = new Scanner(reasoningContentDisplayBuf.toString())) {
                while (scanner.hasNextLine()) {
                    displayBuf.append("> %s\n".formatted(scanner.nextLine()));
                }
            }
        }
        final var contentDisplayBuf = context.getContentDisplayBuf();
        if (StringUtils.isNoneBlank(contentDisplayBuf)) {
            displayBuf
                    .append(displayBuf.isEmpty() ? "" : "\n")
                    .append(contentDisplayBuf);
        }
        return displayBuf;
    }


    private void renderingResponseMessageViewOnNext(MossChatRenderingContext context, ChatResponse response) {

        // 更新检索引用
        final var referenceBuf = context.getReferenceDisplayBuf();
        if (referenceBuf.isEmpty() && response.output().hasSearchInfo()) {
            response.output().searchInfo().results()
                    .forEach(result -> referenceBuf.append("> - [%s](%s)\n".formatted(
                            result.title(),
                            result.site()
                    )));
        }

        final var responseMessage = response.output().best().message();

        // 更新内容缓存
        final var contentDisplayBuf = context.getContentDisplayBuf();
        final var content = responseMessage.text();
        if (StringUtils.isNoneBlank(content)) {
            contentDisplayBuf.append(content);
        }

        // 更新思考缓存
        final var reasoningContentDisplayBuf = context.getReasoningContentDisplayBuf();
        final var reasoningContent = responseMessage.reasoningContent();
        if (StringUtils.isNoneBlank(reasoningContent)) {
            reasoningContentDisplayBuf.append(reasoningContent);
        }

        final var responseMessageView = context.getResponseMessageView();
        final var responseDisplayBuf = renderingResponseDisplayBuf(context);
        Platform.runLater(() -> responseMessageView.setContent(responseDisplayBuf));
    }

    private void renderingResponseMessageViewOnFinish(MossChatRenderingContext context) {
        final var referenceBuf = context.getReferenceDisplayBuf();
        final var responseMessageView = context.getResponseMessageView();
        final var responseDisplayBuf = renderingResponseDisplayBuf(context)
                .append("\n")
                .append(referenceBuf);
        Platform.runLater(() -> {
            responseMessageView.setContent(responseDisplayBuf);
            responseMessageView.setButtonBarEnabled(true);
            if (context.getSource() == enterToggleButton) {
                enterToggleButton.setSelected(false);
            }
        });
    }

    private void renderingResponseMessageViewOnError(MossChatRenderingContext context, Throwable ex) {
        final var responseMessageView = context.getResponseMessageView();
        final var contentDisplayBuf = context.getContentDisplayBuf();
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
        final var responseDisplayBuf = renderingResponseDisplayBuf(context)
                .append("\n")
                .append(error);
        Platform.runLater(() -> {
            responseMessageView.setContent(responseDisplayBuf);
            responseMessageView.setButtonBarEnabled(true);
            if (context.getSource() == enterToggleButton) {
                enterToggleButton.setSelected(false);
            }
        });
    }

}
