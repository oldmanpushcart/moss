package io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.impl.splitter;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class TextSplitByRecursiveCharacter implements TextSplitter {

    private final int size;
    private final int overlap;

    @Override
    public List<Chunk> split(String text) {
        final var chunks = new ArrayList<Chunk>();
        int start = 0;
        while (start < text.length()) {
            final var end = Math.min(start + size, text.length());
            final var chunk = new Chunk(start, end - start);
            chunks.add(chunk);
            start += (size - overlap);
        }
        return chunks;
    }

}
