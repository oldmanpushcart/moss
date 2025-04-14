package io.github.oldmanpushcart.moss.backend.knowledge.internal.manager.impl.splitter;

import java.util.List;

public interface TextSplitter {

    List<Chunk> split(String text);

    record Chunk(int position, int length) {

        public String text(String text) {
            return text.substring(position, position + length);
        }

        public int start() {
            return position;
        }

        public int end() {
            return position + length;
        }

    }

}
