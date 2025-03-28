package io.github.oldmanpushcart.moss.infra;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.moss.SpringBootSupport;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import io.github.oldmanpushcart.moss.infra.memory.MemoryFragment;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@AllArgsConstructor(onConstructor_ = @Autowired)
public class MemoryTestCase extends SpringBootSupport {

    private final Memory memory;

    @Test
    public void test$() {

        for(int index = 0; index < 10; index++) {
            final var fragment = new MemoryFragment()
                    .uuid(UUID.randomUUID().toString())
                    .requestMessage(Message.ofUser("REQUEST: %s!".formatted(index)))
                    .responseMessage(Message.ofAi("RESPONSE: %s!".formatted(index)));
            memory.saveOrUpdate(fragment);
        }

    }

    @Test
    public void test$recall() {
        memory.recall().forEach(System.out::println);
    }

}
