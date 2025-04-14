package io.github.oldmanpushcart.moss;

import io.github.oldmanpushcart.moss.backend.knowledge.Knowledge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class DebugMossTestCase extends SpringSupported {

    @Autowired
    private Knowledge knowledge;

    @Test
    public void debug() throws IOException {
        knowledge.matches("""
                        明天天气如何？
                        """)
                .thenAccept(result-> {
                    for (final var item : result.items()) {
                        System.out.println(item);
                    }
                })
                .toCompletableFuture()
                .join();
    }

}
