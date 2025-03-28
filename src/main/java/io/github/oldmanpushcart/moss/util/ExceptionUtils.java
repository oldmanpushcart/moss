package io.github.oldmanpushcart.moss.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.nonNull;

public class ExceptionUtils {

    /**
     * 获取异常堆栈信息到文本
     *
     * @param ex 异常
     * @return 异常堆栈信息文本
     */
    public static String stackTraceToString(Throwable ex) {
        final var writer = new StringWriter();
        try (final var printer = new PrintWriter(writer)) {
            ex.printStackTrace(printer);
            return writer.toString();
        }
    }

    /**
     * 提取底层异常
     * <p>
     * 一些框架会倾向于将异常包装，
     * 比如{@link CompletableFuture}会将运行过程中的错误封装成{@link CompletionException}或者{@link ExecutionException}，
     * 这种时候异常堆栈输出、界面展示上会出现大量不必要的堆栈信息。<br/>
     * 所以这里提供一个方法，让我们提取底层异常，
     * </p>
     *
     * @param ex 异常信息
     * @return 底层异常
     */
    public static Throwable resolveRootCause(Throwable ex) {
        if ((ex instanceof CompletionException || ex instanceof ExecutionException) && nonNull(ex.getCause())) {
            return resolveRootCause(ex.getCause());
        } else {
            return ex;
        }
    }

}
