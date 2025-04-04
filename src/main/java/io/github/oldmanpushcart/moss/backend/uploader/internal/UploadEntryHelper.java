package io.github.oldmanpushcart.moss.backend.uploader.internal;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.moss.backend.uploader.Uploader;
import io.github.oldmanpushcart.moss.backend.uploader.internal.domain.UploadEntryDO;

class UploadEntryHelper {

    public static Uploader.Entry toUploadEntry(UploadEntryDO entryDO) {
        return new Uploader.Entry(
                entryDO.getEntryId(),
                entryDO.getMime(),
                entryDO.getLength(),
                entryDO.getModel(),
                entryDO.getSource(),
                entryDO.getUpload(),
                entryDO.getExpiresAt(),
                entryDO.getCreatedAt(),
                entryDO.getUpdatedAt()
        );
    }

    public static boolean isQwenLong(String model) {
        return ChatModel.QWEN_LONG.name().equals(model);
    }

    public static boolean isQwenLong(Model model) {
        return isQwenLong(model.name());
    }

}
