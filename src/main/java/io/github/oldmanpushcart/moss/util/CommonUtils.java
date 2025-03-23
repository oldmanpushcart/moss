package io.github.oldmanpushcart.moss.util;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class CommonUtils {

    public static <T> boolean testIfNonNull(T t, Predicate<T> checker) {
        return null != t && checker.test(t);
    }

    public static <T> void acceptIfNonNull(T t, Consumer<T> consumer) {
        if (null != t) {
            consumer.accept(t);
        }
    }

    public static <T extends Comparable<T>> T min(T a, T b) {
        return a.compareTo(b) < 0 ? a : b;
    }

    @SafeVarargs
    public static <T> boolean isIn(T t, T... ts) {
        if(null == ts) {
            return false;
        }
        for (final T t1 : ts) {
            if (t1.equals(t)) {
                return true;
            }
        }
        return false;
    }

}
