package io.github.oldmanpushcart.moss.manager;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.moss.infra.memory.MemoryFragment;
import io.reactivex.rxjava3.core.Flowable;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface ChatManager {

    CompletionStage<Flowable<ChatResponse>> chat(MemoryFragment fragment, List<File> attachments);

}
