package io.github.oldmanpushcart.moss.infra.uploader.internal;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.moss.infra.uploader.UploadEntry;
import io.github.oldmanpushcart.moss.infra.uploader.internal.domain.UploadEntryDO;

class UploadEntryHelper {

    public static UploadEntry toUploadEntry(UploadEntryDO entryDO) {
        return new UploadEntry(
                entryDO.getEntryId(),
                entryDO.getUpload(),
                entryDO.getMime(),
                entryDO.getLength()
        );
    }

    public static boolean isQwenLong(String model) {
        return ChatModel.QWEN_LONG.name().equals(model);
    }

    public static boolean isQwenLong(Model model) {
        return isQwenLong(model.name());
    }

}
