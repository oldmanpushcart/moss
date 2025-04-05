package io.github.oldmanpushcart.moss.backend.uploader.internal;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.moss.backend.uploader.Uploader;
import io.github.oldmanpushcart.moss.backend.uploader.internal.domain.UploadEntryDO;

class UploadEntryHelper {

    public static Uploader.Entry toUploadEntry(UploadEntryDO entryDO) {
        return new Uploader.Entry(
                entryDO.getEntryId(),
                entryDO.getUniqueKey(),
                entryDO.getModel(),
                entryDO.getLength(),
                entryDO.getFilename(),
                entryDO.getUploaded(),
                entryDO.getExpiresAt(),
                entryDO.getCreatedAt()
        );
    }

    public static UploadEntryDO toUploadedEntryDO(FileMeta meta) {
        final var model = ChatModel.QWEN_LONG.name().toLowerCase();
        return new UploadEntryDO()
                .setUniqueKey(computeUniqueKey(model, meta.name()))
                .setFilename(meta.name())
                .setModel(model)
                .setLength(meta.size())
                .setUploadId(meta.identity())
                .setUploaded(meta.toURI())
                .setCreatedAt(meta.uploadedAt());
    }

    public static String computeModel(Model model) {
        return model.name().toLowerCase();
    }

    public static String computeUniqueKey(String model, String filename) {
        return "%s-%s".formatted(model, filename);
    }

    public static String computeUniqueKey(Model model, String filename) {
        return computeUniqueKey(computeModel(model), filename);
    }

    public static boolean isQwenLong(String model) {
        return computeModel(ChatModel.QWEN_LONG).equals(model);
    }

    public static boolean isQwenLong(Model model) {
        return isQwenLong(model.name());
    }

}
