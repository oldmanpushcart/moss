package io.github.oldmanpushcart.moss.frontend.javafx.controller.chat;

import io.github.oldmanpushcart.moss.frontend.javafx.view.MessageView;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 聊天渲染上下文
 */
@Data
@Accessors(chain = true)
public class ChatRenderingContext {

    private Object source;
    private String inputText;
    private MessageView requestMessageView;
    private MessageView responseMessageView;
    private StringBuilder referenceDisplayBuf = new StringBuilder();
    private StringBuilder contentDisplayBuf = new StringBuilder();
    private StringBuilder reasoningContentDisplayBuf = new StringBuilder();

    /**
     * 清空显示缓冲区
     */
    public void cleanDisplayBuf() {
        referenceDisplayBuf.setLength(0);
        contentDisplayBuf.setLength(0);
        reasoningContentDisplayBuf.setLength(0);
    }

}
