package io.github.oldmanpushcart.moss.gui.controller.chat;

import io.github.oldmanpushcart.moss.gui.view.MessageView;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MossChatRenderingContext {

    private Object source;
    private MessageView responseMessageView;
    private StringBuilder referenceDisplayBuf = new StringBuilder();
    private StringBuilder contentDisplayBuf = new StringBuilder();
    private StringBuilder reasoningContentDisplayBuf = new StringBuilder();

    public MossChatRenderingContext cleanDisplayBuf() {
        referenceDisplayBuf.setLength(0);
        contentDisplayBuf.setLength(0);
        reasoningContentDisplayBuf.setLength(0);
        return this;
    }

}
