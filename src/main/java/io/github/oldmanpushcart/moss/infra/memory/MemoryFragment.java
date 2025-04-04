package io.github.oldmanpushcart.moss.infra.memory;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@Accessors(chain = true, fluent = true)
public class MemoryFragment implements Comparable<MemoryFragment> {

    private Long fragmentId;
    private Long tokens;
    private Message requestMessage;
    private Message responseMessage;
    private Instant createdAt;
    private Instant updatedAt;

    @Override
    public int compareTo(@NotNull MemoryFragment o) {
        return Long.compare(this.fragmentId, o.fragmentId);
    }

}
