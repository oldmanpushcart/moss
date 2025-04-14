package io.github.oldmanpushcart.moss.backend.knowledge;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 知识库
 */
public interface Knowledge {

    /**
     * 知识库匹配
     *
     * @param query  查询语句
     * @param option 匹配选项
     * @return 匹配结果
     */
    CompletionStage<MatchResult> matches(String query, MatchOption option);

    /**
     * 知识库匹配
     *
     * @param query 查询语句
     * @return 匹配结果
     */
    default CompletionStage<MatchResult> matches(String query) {
        return matches(query, new MatchOption());
    }

    /**
     * 匹配选项
     */
    @Data
    @Accessors(chain = true, fluent = true)
    class MatchOption {

        /**
         * 是否开启智能重写
         */
        private boolean rewriteEnabled = true;

        /**
         * 聊天记录
         */
        private List<Message> history = new ArrayList<>();

        /**
         * 匹配数量
         */
        private int limit = 10;

        /**
         * 匹配距离
         */
        private float distance = 1.0f;

    }

    /**
     * 匹配结果
     *
     * @param items 匹配结果项集合
     */
    record MatchResult(
            List<Item> items
    ) {

        public boolean isEmpty() {
            return items.isEmpty();
        }

        /**
         * 匹配结果项
         *
         * @param content  内容
         * @param distance 距离
         */
        public record Item(
                String content,
                float distance
        ) {

        }

    }

}
