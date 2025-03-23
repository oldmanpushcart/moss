package io.github.oldmanpushcart.moss.infra;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.moss.SpringBootSupport;
import io.github.oldmanpushcart.moss.infra.uploader.Uploader;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;

@AllArgsConstructor(onConstructor_ = @Autowired)
public class UploaderTestCase extends SpringBootSupport {

    private final Uploader uploader;

    @Test
    public void test$uploader$upload() {

        final var entry1 = uploader.upload(ChatModel.QWEN_MAX, URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg"))
                .toCompletableFuture()
                .join();

        //uploader.delete(entry1.entryId());

        final var entry2 = uploader.upload(ChatModel.QWEN_LONG, URI.create("https://ompc-storage.oss-cn-hangzhou.aliyuncs.com/dashscope4j/P020210313315693279320.pdf"))
                .toCompletableFuture()
                .join();

        uploader.delete(entry2.entryId());

        try {
            Thread.sleep(1000 * 60 * 10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
