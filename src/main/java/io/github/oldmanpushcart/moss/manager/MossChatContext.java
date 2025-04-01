package io.github.oldmanpushcart.moss.manager;

import io.github.oldmanpushcart.moss.infra.memory.MemoryFragment;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.List;

@Value
@Accessors(chain = true, fluent = true)
@Builder(builderMethodName = "newBuilder")
public class MossChatContext {

    MemoryFragment fragment;
    List<File> attachments;

}
