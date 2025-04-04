package io.github.oldmanpushcart.moss.frontend.controller.chat;

import io.github.oldmanpushcart.moss.frontend.view.MessageView;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ChatRenderingContext {

    private Object source;
    private MessageView responseMessageView;
    private StringBuilder referenceDisplayBuf = new StringBuilder();
    private StringBuilder contentDisplayBuf = new StringBuilder();
    private StringBuilder reasoningContentDisplayBuf = new StringBuilder();

    public ChatRenderingContext cleanDisplayBuf() {
        referenceDisplayBuf.setLength(0);
        contentDisplayBuf.setLength(0);
        reasoningContentDisplayBuf.setLength(0);
        return this;
    }

}
