package io.github.oldmanpushcart.moss.gui.controller.chat;

import io.github.oldmanpushcart.moss.gui.view.MessageView;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.List;

@Data
@Accessors(chain = true)
public class MossChatContext {

    private Object source;
    private List<File> attachments;
    private MessageView responseMessageView;
    private Long timeline;

    private StringBuilder referenceDisplayBuf = new StringBuilder();
    private StringBuilder contentDisplayBuf = new StringBuilder();
    private StringBuilder reasoningContentDisplayBuf = new StringBuilder();

    public MossChatContext cleanDisplayBuf() {
        referenceDisplayBuf.setLength(0);
        contentDisplayBuf.setLength(0);
        reasoningContentDisplayBuf.setLength(0);
        return this;
    }

}
