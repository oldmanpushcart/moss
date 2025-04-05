package io.github.oldmanpushcart.moss.frontend.javafx.controller.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.moss.backend.chatter.Chatter;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import lombok.AllArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 重新发送事件处理器
 */
@AllArgsConstructor
public class OnRedoEventHandler implements EventHandler<ActionEvent> {

    private final AtomicBoolean autoScrollToBottomRef;
    private final ChatRequest request;
    private final Supplier<ChatModel> modelFactory;
    private final Supplier<List<File>> attachmentsFactory;
    private final BiConsumer<ChatRequest, CompositeDisposable> action;

    @Override
    public void handle(ActionEvent event) {

        event.consume();
        autoScrollToBottomRef.set(false);
        final var newRequest = ChatRequest.newBuilder(request)
                .model(modelFactory.get())
                .building(builder -> {
                    final var context = request.context(Chatter.Context.class)
                            .setAttachments(attachmentsFactory.get());
                    final var renderingContext = request.context(ChatRenderingContext.class)
                            .setSource(event.getSource())
                            .cleanDisplayBuf();
                    builder.context(Chatter.Context.class, context);
                    builder.context(ChatRenderingContext.class, renderingContext);
                })
                .build();

        action.accept(newRequest, new CompositeDisposable());

    }

}
