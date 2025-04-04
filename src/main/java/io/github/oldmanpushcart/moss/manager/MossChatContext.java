package io.github.oldmanpushcart.moss.manager;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.List;

@Data
@Accessors(chain = true)
public class MossChatContext {

    private List<File> attachments;
    private Long timeline;

}
