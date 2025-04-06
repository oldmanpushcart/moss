package io.github.oldmanpushcart.moss.frontend.util;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class PlatformUtils {

    public static CompletionStage<?> runLaterOnPlatform(CaughtRunnable runnable) {
        final var completed = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {

                runnable.run();

                /*
                 * 异步处理，避免回调在JavaFx线程中执行
                 */
                completed.completeAsync(() -> null);

            } catch (Throwable ex) {

                /*
                 * 异步处理，避免异常回调在JavaFx的线程中进行
                 */
                CompletableFuture
                        .runAsync(() -> {
                        })
                        .thenAccept(unused -> completed.completeExceptionally(ex));

            }
        });
        return completed;
    }

    public static <T> CompletionStage<T> runLaterOnPlatform(CaughtSupplier<T> runnable) {
        final var completed = new CompletableFuture<T>();
        Platform.runLater(() -> {
            try {

                final var result = runnable.get();

                /*
                 * 异步处理，避免回调在JavaFx线程中执行
                 */
                completed.completeAsync(() -> result);

            } catch (Throwable ex) {

                /*
                 * 异步处理，避免异常回调在JavaFx的线程中进行
                 */
                CompletableFuture
                        .runAsync(() -> {
                        })
                        .thenAccept(unused -> completed.completeExceptionally(ex));

            }
        });
        return completed;
    }

    /**
     * 运行在JavaFx线程中
     */
    public interface CaughtRunnable {

        /**
         * 运行
         *
         * @throws Throwable 运行时异常
         */
        void run() throws Throwable;

    }

    public interface CaughtSupplier<T> {

        /**
         * 运行
         *
         * @throws Throwable 运行时异常
         */
        T get() throws Throwable;

    }

}
